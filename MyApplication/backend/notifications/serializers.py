from rest_framework import serializers

from accounts.serializers import UserSerializer
from .models import Notification


class NotificationSerializer(serializers.ModelSerializer):
    actor = UserSerializer(read_only=True)

    class Meta:
        model = Notification
        fields = ["id", "type", "text", "actor", "related_object_id", "is_read", "created_at"]




