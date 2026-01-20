from rest_framework import serializers

from accounts.serializers import UserSerializer
from .models import Post, Comment, Story, StoryReply


class CommentSerializer(serializers.ModelSerializer):
    author = UserSerializer(read_only=True)
    like_count = serializers.SerializerMethodField()

    class Meta:
        model = Comment
        fields = [
            "id",
            "post",
            "author",
            "content",
            "created_at",
            "parent",
            "like_count",
        ]
        read_only_fields = ("post", "author", "created_at", "like_count")

    def get_like_count(self, obj):
        return obj.likes.count()


class PostSerializer(serializers.ModelSerializer):
    author = UserSerializer(read_only=True)
    like_count = serializers.SerializerMethodField()
    comment_count = serializers.SerializerMethodField()
    is_liked = serializers.SerializerMethodField()
    is_author = serializers.SerializerMethodField()
    event_id = serializers.IntegerField(source="event.id", read_only=True, allow_null=True)

    class Meta:
        model = Post
        fields = [
            "id",
            "author",
            "content",
            "image_url",
            "event",
            "event_id",
            "created_at",
            "updated_at",
            "like_count",
            "comment_count",
            "is_liked",
            "is_author",
        ]
        read_only_fields = (
            "author",
            "created_at",
            "updated_at",
            "like_count",
            "comment_count",
            "is_liked",
            "is_author",
            "event_id",
        )

    def get_like_count(self, obj):
        return obj.likes.count()

    def get_comment_count(self, obj):
        return obj.comments.count()

    def get_is_liked(self, obj):
        user = self.context.get("request").user
        if not user or not user.is_authenticated:
            return False
        return obj.likes.filter(user=user).exists()

    def get_is_author(self, obj):
        user = self.context.get("request").user
        if not user or not user.is_authenticated:
            return False
        return obj.author == user


class StoryReplySerializer(serializers.ModelSerializer):
    author = UserSerializer(read_only=True)

    class Meta:
        model = StoryReply
        fields = [
            "id",
            "story",
            "author",
            "content",
            "created_at",
        ]
        read_only_fields = ("author", "created_at")


class StorySerializer(serializers.ModelSerializer):
    author = UserSerializer(read_only=True)
    like_count = serializers.SerializerMethodField()
    is_liked = serializers.SerializerMethodField()
    reply_count = serializers.SerializerMethodField()

    class Meta:
        model = Story
        fields = [
            "id",
            "author",
            "image_url",
            "created_at",
            "expires_at",
            "like_count",
            "is_liked",
            "reply_count",
        ]
        read_only_fields = ("author", "created_at", "expires_at", "like_count", "is_liked", "reply_count")

    def get_like_count(self, obj):
        return obj.likes.count()

    def get_is_liked(self, obj):
        user = self.context.get("request").user
        if not user or not user.is_authenticated:
            return False
        return obj.likes.filter(user=user).exists()

    def get_reply_count(self, obj):
        return obj.replies.count()

