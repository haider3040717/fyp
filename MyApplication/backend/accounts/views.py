from rest_framework import generics, permissions, status
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.decorators import action
from django.db.models import Q
from django.conf import settings
from rest_framework_simplejwt.tokens import RefreshToken

from .models import User, Profile, Follow
from .serializers import (
    RegisterSerializer,
    LoginSerializer,
    UserSerializer,
    ProfileSerializer,
)


class RegisterView(generics.CreateAPIView):
    serializer_class = RegisterSerializer
    permission_classes = [permissions.AllowAny]


class LoginView(APIView):
    permission_classes = [permissions.AllowAny]

    def post(self, request, *args, **kwargs):
        serializer = LoginSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        user = serializer.validated_data["user"]
        
        # Generate JWT tokens
        refresh = RefreshToken.for_user(user)
        access_token = str(refresh.access_token)
        refresh_token = str(refresh)
        
        return Response(
            {
                "access": access_token,
                "refresh": refresh_token,
                "user": UserSerializer(user, context={"request": request}).data,
            }
        )


class MeView(generics.RetrieveAPIView):
    serializer_class = UserSerializer

    def get_object(self):
        return self.request.user

    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["request"] = self.request
        return context


class ProfileView(generics.RetrieveUpdateAPIView):
    serializer_class = ProfileSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_object(self):
        profile, _ = Profile.objects.get_or_create(user=self.request.user)
        return profile


class UserProfileView(generics.RetrieveAPIView):
    """
    Get any user's profile by ID.
    GET /api/accounts/users/{user_id}/
    """
    serializer_class = UserSerializer
    permission_classes = [permissions.IsAuthenticated]
    queryset = User.objects.all()
    lookup_field = "pk"

    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["request"] = self.request
        return context


class ProfileShareView(APIView):
    """
    Get a shareable URL for a user's profile.
    GET /api/accounts/profile/{user_id}/share/
    """
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
            share_url = f"{settings.FRONTEND_BASE_URL}/profile/{user.id}"
            return Response({"share_url": share_url})
        except User.DoesNotExist:
            return Response(
                {"detail": "User not found"},
                status=status.HTTP_404_NOT_FOUND
            )


class FollowView(APIView):
    """
    Follow/unfollow a user.
    POST /api/accounts/follow/{user_id}/ -> follow user
    DELETE /api/accounts/follow/{user_id}/ -> unfollow user
    """
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, user_id):
        try:
            target_user = User.objects.get(id=user_id)
            if target_user == request.user:
                return Response(
                    {"detail": "Cannot follow yourself"},
                    status=status.HTTP_400_BAD_REQUEST
                )
            created = Follow.objects.get_or_create(
                follower=request.user,
                following=target_user
            )[1]
            # Create notification if follow was just created
            if created:
                from notifications.models import Notification
                Notification.objects.create(
                    user=target_user,
                    actor=request.user,
                    type="follow",
                    text=f"{request.user.full_name} started following you",
                    related_object_id=request.user.id
                )
            return Response({"status": "following"})
        except User.DoesNotExist:
            return Response(
                {"detail": "User not found"},
                status=status.HTTP_404_NOT_FOUND
            )

    def delete(self, request, user_id):
        try:
            target_user = User.objects.get(id=user_id)
            Follow.objects.filter(
                follower=request.user,
                following=target_user
            ).delete()
            return Response({"status": "unfollowed"})
        except User.DoesNotExist:
            return Response(
                {"detail": "User not found"},
                status=status.HTTP_404_NOT_FOUND
            )


class FriendRequestsView(APIView):
    """
    Get friend requests (users who follow you but you don't follow back).
    GET /api/accounts/friend-requests/ -> list pending friend requests
    """
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        # Users who follow me but I don't follow back
        followers = Follow.objects.filter(following=request.user).values_list("follower_id", flat=True)
        following = Follow.objects.filter(follower=request.user).values_list("following_id", flat=True)
        pending_ids = set(followers) - set(following)
        users = User.objects.filter(id__in=pending_ids).select_related("profile")
        serializer = UserSerializer(users, many=True)
        return Response(serializer.data)


class FriendSuggestionsView(APIView):
    """
    Get friend suggestions (users you might know).
    GET /api/accounts/friend-suggestions/ -> list friend suggestions
    """
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        # Users not followed by current user, excluding self
        following_ids = Follow.objects.filter(
            follower=request.user
        ).values_list("following_id", flat=True)
        suggestions = User.objects.exclude(
            id__in=list(following_ids) + [request.user.id]
        ).select_related("profile")[:20]  # Limit to 20 suggestions
        serializer = UserSerializer(suggestions, many=True)
        return Response(serializer.data)


class SearchView(APIView):
    """
    Search users and posts.
    GET /api/accounts/search/?q=<query>&type=users -> search users
    GET /api/accounts/search/?q=<query>&type=posts -> search posts
    """
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        query = request.query_params.get("q", "").strip()
        search_type = request.query_params.get("type", "users")

        if not query:
            return Response({"users": [], "posts": []})

        if search_type == "users":
            users = User.objects.filter(
                Q(full_name__icontains=query) |
                Q(seat_number__icontains=query) |
                Q(department__icontains=query)
            ).select_related("profile")[:20]
            user_serializer = UserSerializer(users, many=True)
            return Response({"users": user_serializer.data, "posts": []})
        elif search_type == "posts":
            from posts.models import Post
            from posts.serializers import PostSerializer
            posts = Post.objects.filter(
                content__icontains=query
            ).select_related("author").order_by("-created_at")[:20]
            post_serializer = PostSerializer(posts, many=True, context={"request": request})
            return Response({"users": [], "posts": post_serializer.data})
        else:
            # Return both
            users = User.objects.filter(
                Q(full_name__icontains=query) |
                Q(seat_number__icontains=query) |
                Q(department__icontains=query)
            ).select_related("profile")[:10]
            from posts.models import Post
            from posts.serializers import PostSerializer
            posts = Post.objects.filter(
                content__icontains=query
            ).select_related("author").order_by("-created_at")[:10]
            user_serializer = UserSerializer(users, many=True)
            post_serializer = PostSerializer(posts, many=True, context={"request": request})
            return Response({
                "users": user_serializer.data,
                "posts": post_serializer.data
            })


class UpdateEmailView(APIView):
    """
    Update user's email address.
    PATCH /api/accounts/update-email/ -> update email
    """
    permission_classes = [permissions.IsAuthenticated]

    def patch(self, request):
        # Accept both "email" and "university_email" for compatibility
        new_email = request.data.get("university_email", request.data.get("email", "")).strip()
        if not new_email:
            return Response(
                {"detail": "Email is required"},
                status=status.HTTP_400_BAD_REQUEST
            )
        # Basic email validation
        if "@" not in new_email:
            return Response(
                {"detail": "Invalid email format"},
                status=status.HTTP_400_BAD_REQUEST
            )
        request.user.university_email = new_email
        request.user.save()
        return Response({"status": "ok", "university_email": new_email})


class ChangePasswordView(APIView):
    """
    Change user's password.
    POST /api/accounts/change-password/ -> change password
    """
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request):
        old_password = request.data.get("old_password", "")
        new_password = request.data.get("new_password", "")
        confirm_new_password = request.data.get("confirm_new_password", "")
        
        if not old_password or not new_password:
            return Response(
                {"detail": "Both old_password and new_password are required"},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Verify old password
        if not request.user.check_password(old_password):
            return Response(
                {"detail": "Invalid old password"},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Validate new password
        if len(new_password) < 8:
            return Response(
                {"detail": "New password must be at least 8 characters long"},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Validate password confirmation if provided
        if confirm_new_password and new_password != confirm_new_password:
            return Response(
                {"detail": "New passwords do not match"},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Set new password
        request.user.set_password(new_password)
        request.user.save()
        return Response({"status": "ok"})






# API_KEY = "AIzaSyAa1eIl12sdSjsNJ8-ppsljZoMLnYSeFg8"