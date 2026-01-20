from django.db import models
from django.contrib.auth.models import AbstractUser, BaseUserManager
from django.conf import settings


class UserManager(BaseUserManager):
    use_in_migrations = True

    def _create_user(self, seat_number, password, **extra_fields):
        if not seat_number:
            raise ValueError("The seat number must be set")
        seat_number = self.normalize_email(seat_number) if "@" in seat_number else seat_number
        user = self.model(seat_number=seat_number, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_user(self, seat_number, password=None, **extra_fields):
        extra_fields.setdefault("is_staff", False)
        extra_fields.setdefault("is_superuser", False)
        return self._create_user(seat_number, password, **extra_fields)

    def create_superuser(self, seat_number, password, **extra_fields):
        extra_fields.setdefault("is_staff", True)
        extra_fields.setdefault("is_superuser", True)

        if extra_fields.get("is_staff") is not True:
            raise ValueError("Superuser must have is_staff=True.")
        if extra_fields.get("is_superuser") is not True:
            raise ValueError("Superuser must have is_superuser=True.")

        return self._create_user(seat_number, password, **extra_fields)


class User(AbstractUser):
    """
    Custom user model where username is the student's seat number.
    """
    username = None  # remove default username
    seat_number = models.CharField(max_length=50, unique=True)
    full_name = models.CharField(max_length=255)
    university_email = models.EmailField(blank=True, null=True)
    year = models.PositiveIntegerField(blank=True, null=True)
    department = models.CharField(max_length=100, blank=True)

    USERNAME_FIELD = "seat_number"
    REQUIRED_FIELDS = ["full_name"]

    objects = UserManager()

    def __str__(self):
        return f"{self.seat_number} - {self.full_name}"


class Follow(models.Model):
    """
    Follower / following relationship between users.
    """
    follower = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="following", on_delete=models.CASCADE
    )
    following = models.ForeignKey(
        settings.AUTH_USER_MODEL, related_name="followers", on_delete=models.CASCADE
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("follower", "following")

    def __str__(self):
        return f"{self.follower_id} -> {self.following_id}"


class Profile(models.Model):
    """
    Extra profile information for the user.
    """
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name="profile")
    bio = models.TextField(blank=True)
    avatar_url = models.URLField(blank=True)
    course = models.CharField(max_length=255, blank=True)
    interests = models.TextField(blank=True)

    def __str__(self):
        return f"Profile({self.user.seat_number})"

# Create your models here.
