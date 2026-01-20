from rest_framework import viewsets, permissions, status, serializers
from rest_framework.decorators import action
from rest_framework.response import Response
from django.utils import timezone
from datetime import timedelta

from .models import Post, Comment, PostLike, CommentLike, Story, StoryLike, StoryReply
from .serializers import PostSerializer, CommentSerializer, StorySerializer, StoryReplySerializer


class PostViewSet(viewsets.ModelViewSet):
    """
    Endpoints:
    - GET /api/posts/posts/           -> feed (all posts)
    - POST /api/posts/posts/          -> create post
    - GET /api/posts/posts/{id}/      -> post detail
    - POST /api/posts/posts/{id}/like/   -> like post
    - POST /api/posts/posts/{id}/unlike/ -> unlike post
    """

    serializer_class = PostSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        qs = Post.objects.select_related("author", "event").prefetch_related("likes").order_by("-created_at")
        # Filter by author if user_id query param is provided
        user_id = self.request.query_params.get("user_id")
        if user_id:
            qs = qs.filter(author_id=user_id)
        return qs

    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["request"] = self.request
        return context

    def perform_create(self, serializer):
        # Handle event linking if event_id is provided
        event_id = self.request.data.get("event_id")
        if event_id:
            from events.models import Event
            try:
                event = Event.objects.get(id=event_id)
                serializer.save(author=self.request.user, event=event)
            except Event.DoesNotExist:
                serializer.save(author=self.request.user)
        else:
            serializer.save(author=self.request.user)

    @action(detail=True, methods=["post"])
    def like(self, request, pk=None):
        post = self.get_object()
        created = PostLike.objects.get_or_create(user=request.user, post=post)[1]
        # Create notification if like was just created and not by post author
        if created and post.author != request.user:
            from notifications.models import Notification
            Notification.objects.create(
                user=post.author,
                actor=request.user,
                type="like",
                text=f"{request.user.full_name} liked your post",
                related_object_id=post.id
            )
        return Response({"status": "liked"})

    @action(detail=True, methods=["post"])
    def unlike(self, request, pk=None):
        post = self.get_object()
        PostLike.objects.filter(user=request.user, post=post).delete()
        return Response({"status": "unliked"})

    def update(self, request, *args, **kwargs):
        post = self.get_object()
        if post.author != request.user:
            return Response(
                {"detail": "You do not have permission to edit this post."},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().update(request, *args, **kwargs)

    def destroy(self, request, *args, **kwargs):
        post = self.get_object()
        if post.author != request.user:
            return Response(
                {"detail": "You do not have permission to delete this post."},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().destroy(request, *args, **kwargs)

    @action(detail=True, methods=["get"])
    def share_url(self, request, pk=None):
        """Generate a shareable URL for the post."""
        post = self.get_object()
        # In production, use actual domain. For now, use request.build_absolute_uri
        share_url = request.build_absolute_uri(f"/api/posts/posts/{post.id}/")
        return Response({"share_url": share_url, "post_id": post.id})


class CommentViewSet(viewsets.ModelViewSet):
    """
    Endpoints:
    - GET /api/posts/comments/?post=<post_id> -> list comments for a post
    - POST /api/posts/comments/ with post=<id> -> create comment on post
    - POST /api/posts/comments/{id}/like/   -> like comment
    - POST /api/posts/comments/{id}/unlike/ -> unlike comment
    """

    serializer_class = CommentSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        qs = Comment.objects.select_related("author", "post").prefetch_related("likes").order_by("created_at")
        post_id = self.request.query_params.get("post")
        if post_id:
            qs = qs.filter(post_id=post_id, parent__isnull=True)
        return qs

    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["request"] = self.request
        return context

    def perform_create(self, serializer):
        post_id = self.request.data.get("post")
        if not post_id:
            raise serializers.ValidationError({"post": "post is required"})
        comment = serializer.save(author=self.request.user, post_id=post_id)
        # Create notification if comment is not by post author
        post = comment.post
        if post.author != self.request.user:
            from notifications.models import Notification
            Notification.objects.create(
                user=post.author,
                actor=self.request.user,
                type="comment",
                text=f"{self.request.user.full_name} commented on your post",
                related_object_id=post.id
            )

    @action(detail=True, methods=["post"])
    def like(self, request, pk=None):
        comment = self.get_object()
        CommentLike.objects.get_or_create(user=request.user, comment=comment)
        return Response({"status": "liked"})

    @action(detail=True, methods=["post"])
    def unlike(self, request, pk=None):
        comment = self.get_object()
        CommentLike.objects.filter(user=request.user, comment=comment).delete()
        return Response({"status": "unliked"})

    def destroy(self, request, *args, **kwargs):
        comment = self.get_object()
        if comment.author != request.user:
            return Response(
                {"detail": "You do not have permission to delete this comment."},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().destroy(request, *args, **kwargs)


class StoryViewSet(viewsets.ModelViewSet):
    """
    Endpoints:
    - GET /api/posts/stories/           -> list active stories (from followed users)
    - POST /api/posts/stories/          -> create story
    - DELETE /api/posts/stories/{id}/    -> delete story
    - POST /api/posts/stories/{id}/like/ -> like story
    - POST /api/posts/stories/{id}/unlike/ -> unlike story
    """

    serializer_class = StorySerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        # Return active stories (not expired) from users the current user follows
        from accounts.models import Follow
        following_ids = Follow.objects.filter(
            follower=self.request.user
        ).values_list("following_id", flat=True)
        # Include current user's own stories
        following_ids = list(following_ids) + [self.request.user.id]
        return Story.objects.filter(
            author_id__in=following_ids,
            expires_at__gt=timezone.now()
        ).select_related("author").prefetch_related("likes", "replies").order_by("-created_at")

    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["request"] = self.request
        return context

    def perform_create(self, serializer):
        # Stories expire after 24 hours
        expires_at = timezone.now() + timedelta(hours=24)
        serializer.save(author=self.request.user, expires_at=expires_at)

    @action(detail=True, methods=["post"])
    def like(self, request, pk=None):
        story = self.get_object()
        StoryLike.objects.get_or_create(user=request.user, story=story)
        return Response({"status": "liked"})

    @action(detail=True, methods=["post"])
    def unlike(self, request, pk=None):
        story = self.get_object()
        StoryLike.objects.filter(user=request.user, story=story).delete()
        return Response({"status": "unliked"})


class StoryReplyViewSet(viewsets.ModelViewSet):
    """
    Endpoints:
    - GET /api/posts/story-replies/?story=<story_id> -> list replies for a story
    - POST /api/posts/story-replies/ with story=<id> -> create reply on story
    - DELETE /api/posts/story-replies/{id}/ -> delete reply (author only)
    """

    serializer_class = StoryReplySerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        qs = StoryReply.objects.select_related("author", "story").order_by("created_at")
        story_id = self.request.query_params.get("story")
        if story_id:
            qs = qs.filter(story_id=story_id)
        return qs

    def perform_create(self, serializer):
        story_id = self.request.data.get("story")
        if not story_id:
            raise serializers.ValidationError({"story": "story is required"})
        serializer.save(author=self.request.user, story_id=story_id)

    def destroy(self, request, *args, **kwargs):
        reply = self.get_object()
        if reply.author != request.user:
            return Response(
                {"detail": "You do not have permission to delete this reply."},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().destroy(request, *args, **kwargs)
