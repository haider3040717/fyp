from django.conf import settings
from django.db import models


class Event(models.Model):
    """
    University events model.
    """
    title = models.CharField(max_length=255)
    description = models.TextField()
    location = models.CharField(max_length=255)
    latitude = models.DecimalField(max_digits=9, decimal_places=6, default=24.8607)
    longitude = models.DecimalField(max_digits=9, decimal_places=6, default=67.0011)
    start_date = models.DateTimeField()
    end_date = models.DateTimeField()
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="created_events", on_delete=models.CASCADE
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"Event({self.id}): {self.title}"


class EventAttendance(models.Model):
    """
    Track users who are interested or going to events.
    """
    STATUS_CHOICES = [
        ("interested", "Interested"),
        ("going", "Going"),
    ]

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="event_attendances", on_delete=models.CASCADE
    )
    event = models.ForeignKey(
        Event, related_name="attendances", on_delete=models.CASCADE
    )
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default="interested")
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("user", "event")

    def __str__(self):
        return f"{self.user_id} - {self.event_id} ({self.status})"
