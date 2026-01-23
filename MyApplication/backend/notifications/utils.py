# notifications/utils.py
from firebase_admin import messaging
from .models import FCMDevice

def send_push_notification(user, title, body, data=None):
    tokens = FCMDevice.objects.filter(user=user).values_list("token", flat=True)

    if not tokens:
        return

    message = messaging.MulticastMessage(
        notification=messaging.Notification(
            title=title,
            body=body,
        ),
        data=data or {},
        tokens=list(tokens),
    )

    response = messaging.send_multicast(message)
    return response
