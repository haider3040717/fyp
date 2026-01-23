from django.urls import path

from .views import NotificationListView, MarkAllReadView, MarkAsReadView, RegisterFCMTokenView

urlpatterns = [
    path("", NotificationListView.as_view(), name="notifications"),
    path("mark-all-read/", MarkAllReadView.as_view(), name="notifications-mark-all"),
    path("<int:notification_id>/mark-read/", MarkAsReadView.as_view(), name="notification-mark-read"),
    path("register-token/", RegisterFCMTokenView.as_view()),
]



