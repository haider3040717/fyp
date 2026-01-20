from django.urls import path

from .views import (
    ConversationListView,
    ConversationMessagesView,
    StartConversationView,
)

urlpatterns = [
    path("conversations/", ConversationListView.as_view(), name="conversations"),
    path(
        "conversations/<int:pk>/messages/",
        ConversationMessagesView.as_view(),
        name="conversation-messages",
    ),
    path("start/", StartConversationView.as_view(), name="start-conversation"),
]



