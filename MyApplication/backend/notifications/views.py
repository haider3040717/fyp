from rest_framework import generics, permissions, status
from rest_framework.response import Response
# notifications/views.py
from rest_framework.views import APIView
from rest_framework.permissions import IsAuthenticated

from .models import Notification, FCMDevice
from .serializers import NotificationSerializer, FCMDeviceSerializer


class NotificationListView(generics.ListAPIView):
    serializer_class = NotificationSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return Notification.objects.filter(user=self.request.user).order_by(
            "-created_at"
        )

    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["request"] = self.request
        return context


class MarkAllReadView(generics.GenericAPIView):
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, *args, **kwargs):
        Notification.objects.filter(user=request.user, is_read=False).update(
            is_read=True
        )
        return Response({"status": "ok"})


class MarkAsReadView(generics.GenericAPIView):
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, notification_id):
        try:
            notification = Notification.objects.get(id=notification_id, user=request.user)
            notification.is_read = True
            notification.save()
            return Response({"status": "ok"})
        except Notification.DoesNotExist:
            return Response(
                {"detail": "Notification not found"},
                status=status.HTTP_404_NOT_FOUND
            )

from django.shortcuts import render

class RegisterFCMTokenView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        token = request.data.get("token")
        FCMDevice.objects.get_or_create(
            user=request.user,
            token=token
        )
        return Response({"status": "Token registered"})
