from django.contrib import admin
from .models import Post, Comment, PostLike, CommentLike, Story, StoryLike, StoryReply


@admin.register(Post)
class PostAdmin(admin.ModelAdmin):
    list_display = ("id", "author", "content", "created_at")
    search_fields = ("content", "author__full_name")


@admin.register(Comment)
class CommentAdmin(admin.ModelAdmin):
    list_display = ("id", "post", "author", "content", "created_at")
    search_fields = ("content", "author__full_name")


@admin.register(Story)
class StoryAdmin(admin.ModelAdmin):
    list_display = ("id", "author", "created_at", "expires_at")
    search_fields = ("author__full_name",)


@admin.register(StoryLike)
class StoryLikeAdmin(admin.ModelAdmin):
    list_display = ("id", "user", "story", "created_at")
    list_filter = ("created_at",)


@admin.register(StoryReply)
class StoryReplyAdmin(admin.ModelAdmin):
    list_display = ("id", "story", "author", "content", "created_at")
    search_fields = ("content", "author__full_name")
