from rest_framework import serializers
from django.contrib.auth import authenticate

from .models import User, Profile


class ProfileSerializer(serializers.ModelSerializer):
    class Meta:
        model = Profile
        fields = ["bio", "avatar_url", "course", "interests"]


class UserSerializer(serializers.ModelSerializer):
    profile = ProfileSerializer(read_only=True)
    posts_count = serializers.SerializerMethodField()
    friends_count = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = [
            "id",
            "seat_number",
            "full_name",
            "university_email",
            "year",
            "department",
            "profile",
            "posts_count",
            "friends_count",
        ]

    def get_posts_count(self, obj):
        return obj.posts.count()

    def get_friends_count(self, obj):
        return obj.friends_count


class RegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True)

    class Meta:
        model = User
        fields = ["seat_number", "full_name", "password", "university_email"]

    def create(self, validated_data):
        password = validated_data.pop("password")
        user = User.objects.create(**validated_data)
        user.set_password(password)
        user.save()
        Profile.objects.create(user=user)
        return user


class LoginSerializer(serializers.Serializer):
    seat_number = serializers.CharField()
    password = serializers.CharField(write_only=True)

    def validate(self, attrs):
        seat_number = attrs.get("seat_number")
        password = attrs.get("password")
        user = authenticate(request=self.context.get("request"), seat_number=seat_number, password=password)
        if not user:
            raise serializers.ValidationError("Invalid seat number or password")
        attrs["user"] = user
        return attrs




