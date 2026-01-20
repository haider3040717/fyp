from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

from . import views

urlpatterns = [
    path("register/", views.RegisterView.as_view(), name="register"),
    path("login/", views.LoginView.as_view(), name="login"),
    path("token/refresh/", TokenRefreshView.as_view(), name="token_refresh"),
    path("me/", views.MeView.as_view(), name="me"),
    path("profile/", views.ProfileView.as_view(), name="profile"),
    path("users/<int:pk>/", views.UserProfileView.as_view(), name="user_profile"),
    path("profile/<int:user_id>/share/", views.ProfileShareView.as_view(), name="profile_share"),
    path("update-email/", views.UpdateEmailView.as_view(), name="update_email"),
    path("change-password/", views.ChangePasswordView.as_view(), name="change_password"),
    path("follow/<int:user_id>/", views.FollowView.as_view(), name="follow"),
    path("friend-requests/", views.FriendRequestsView.as_view(), name="friend-requests"),
    path("friend-suggestions/", views.FriendSuggestionsView.as_view(), name="friend-suggestions"),
    path("search/", views.SearchView.as_view(), name="search"),
]




