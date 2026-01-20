from django.urls import path, include
from rest_framework.routers import DefaultRouter

from .views import PostViewSet, CommentViewSet, StoryViewSet, StoryReplyViewSet

router = DefaultRouter()
router.register("posts", PostViewSet, basename="post")
router.register("comments", CommentViewSet, basename="comment")
router.register("stories", StoryViewSet, basename="story")
router.register("story-replies", StoryReplyViewSet, basename="story-reply")

urlpatterns = [
    path("", include(router.urls)),
]



