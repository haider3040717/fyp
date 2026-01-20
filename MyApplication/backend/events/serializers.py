from rest_framework import serializers
from .models import Event, EventAttendance
from accounts.serializers import UserSerializer


class EventSerializer(serializers.ModelSerializer):
    created_by = UserSerializer(read_only=True)
    interested_count = serializers.SerializerMethodField()
    going_count = serializers.SerializerMethodField()
    is_interested = serializers.SerializerMethodField()
    is_going = serializers.SerializerMethodField()

    class Meta:
        model = Event
        fields = [
            "id",
            "title",
            "description",
            "location",
            "latitude",
            "longitude",
            "start_date",
            "end_date",
            "created_by",
            "created_at",
            "updated_at",
            "interested_count",
            "going_count",
            "is_interested",
            "is_going",
        ]

    def get_interested_count(self, obj):
        return obj.attendances.filter(status="interested").count()

    def get_going_count(self, obj):
        return obj.attendances.filter(status="going").count()

    def get_is_interested(self, obj):
        request = self.context.get("request")
        if request and request.user.is_authenticated:
            return obj.attendances.filter(
                user=request.user, status="interested"
            ).exists()
        return False

    def get_is_going(self, obj):
        request = self.context.get("request")
        if request and request.user.is_authenticated:
            return obj.attendances.filter(
                user=request.user, status="going"
            ).exists()
        return False


class EventAttendanceSerializer(serializers.ModelSerializer):
    class Meta:
        model = EventAttendance
        fields = ["id", "user", "event", "status", "created_at"]
        read_only_fields = ["user", "created_at"]



