from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.utils import timezone

from .models import Event, EventAttendance
from .serializers import EventSerializer, EventAttendanceSerializer


class EventViewSet(viewsets.ModelViewSet):
    """
    Endpoints:
    - GET /api/events/events/           -> list all events
    - POST /api/events/events/          -> create event
    - GET /api/events/events/{id}/      -> event detail
    - POST /api/events/events/{id}/interested/ -> mark as interested
    - POST /api/events/events/{id}/going/      -> mark as going
    - POST /api/events/events/{id}/uninterested/ -> remove interest
    """

    serializer_class = EventSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return Event.objects.select_related("created_by").prefetch_related(
            "attendances"
        ).order_by("-start_date")

    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["request"] = self.request
        return context

    def perform_create(self, serializer):
        serializer.save(created_by=self.request.user)

    def perform_destroy(self, instance):
        # Only allow the event creator to delete their events
        if instance.created_by != self.request.user:
            from rest_framework.exceptions import PermissionDenied
            raise PermissionDenied("You can only delete your own events.")
        instance.delete()

    @action(detail=True, methods=["post"])
    def interested(self, request, pk=None):
        event = self.get_object()
        created = EventAttendance.objects.update_or_create(
            user=request.user,
            event=event,
            defaults={"status": "interested"}
        )[1]
        # Create notification if attendance was just created and not by event creator
        if created and event.created_by != request.user:
            from notifications.models import Notification
            Notification.objects.create(
                user=event.created_by,
                actor=request.user,
                type="event",
                text=f"{request.user.full_name} is interested in your event: {event.title}",
                related_object_id=event.id
            )
        return Response({"status": "interested"})

    @action(detail=True, methods=["post"])
    def going(self, request, pk=None):
        event = self.get_object()
        created = EventAttendance.objects.update_or_create(
            user=request.user,
            event=event,
            defaults={"status": "going"}
        )[1]
        # Create notification if attendance was just created and not by event creator
        if created and event.created_by != request.user:
            from notifications.models import Notification
            Notification.objects.create(
                user=event.created_by,
                actor=request.user,
                type="event",
                text=f"{request.user.full_name} is going to your event: {event.title}",
                related_object_id=event.id
            )
        return Response({"status": "going"})

    @action(detail=True, methods=["post"])
    def uninterested(self, request, pk=None):
        event = self.get_object()
        EventAttendance.objects.filter(
            user=request.user, event=event
        ).delete()
        return Response({"status": "uninterested"})
