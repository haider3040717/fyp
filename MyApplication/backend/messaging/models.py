from django.conf import settings
from django.db import models


class Conversation(models.Model):
    """
    Direct chat between two users (for now).
    """
    user1 = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="conversations_as_user1", on_delete=models.CASCADE
    )
    user2 = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="conversations_as_user2", on_delete=models.CASCADE
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("user1", "user2")

    def __str__(self):
        return f"Conversation({self.id})"


class Message(models.Model):
    conversation = models.ForeignKey(
        Conversation, related_name="messages", on_delete=models.CASCADE
    )
    sender = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="sent_messages", on_delete=models.CASCADE
    )
    text = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)
    is_read = models.BooleanField(default=False)

    def __str__(self):
        return f"Message({self.id}) in Conv({self.conversation_id})"

from django.db import models

# Create your models here.
