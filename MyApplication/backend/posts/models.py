from django.conf import settings
from django.db import models


class Post(models.Model):
    author = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="posts", on_delete=models.CASCADE
    )
    content = models.TextField()
    image_url = models.URLField(blank=True)
    event = models.ForeignKey(
        "events.Event", related_name="posts", on_delete=models.SET_NULL, null=True, blank=True
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"Post({self.id}) by {self.author_id}"


class PostLike(models.Model):
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="post_likes", on_delete=models.CASCADE
    )
    post = models.ForeignKey(
        Post, related_name="likes", on_delete=models.CASCADE
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("user", "post")


class Comment(models.Model):
    post = models.ForeignKey(Post, related_name="comments", on_delete=models.CASCADE)
    author = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="comments", on_delete=models.CASCADE
    )
    content = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)
    parent = models.ForeignKey(
        "self", null=True, blank=True, related_name="replies", on_delete=models.CASCADE
    )

    def __str__(self):
        return f"Comment({self.id}) on Post({self.post_id})"


class CommentLike(models.Model):
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="comment_likes", on_delete=models.CASCADE
    )
    comment = models.ForeignKey(
        Comment, related_name="likes", on_delete=models.CASCADE
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("user", "comment")


class Story(models.Model):
    """
    User stories (temporary content, typically expire after 24 hours).
    """
    author = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="stories", on_delete=models.CASCADE
    )
    image_url = models.URLField()
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField()

    def __str__(self):
        return f"Story({self.id}) by {self.author_id}"


class StoryLike(models.Model):
    """
    User likes on stories.
    """
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="story_likes", on_delete=models.CASCADE
    )
    story = models.ForeignKey(
        Story, related_name="likes", on_delete=models.CASCADE
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("user", "story")

    def __str__(self):
        return f"{self.user_id} liked Story({self.story_id})"


class StoryReply(models.Model):
    """
    User replies to stories.
    """
    story = models.ForeignKey(
        Story, related_name="replies", on_delete=models.CASCADE
    )
    author = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="story_replies", on_delete=models.CASCADE
    )
    content = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Reply({self.id}) on Story({self.story_id})"
