from django.db.models import Q
from rest_framework import generics, permissions
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.exceptions import PermissionDenied, NotFound

from .models import Conversation, Message
from .serializers import ConversationSerializer, MessageSerializer


class ConversationListView(generics.ListAPIView):
    serializer_class = ConversationSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        return Conversation.objects.filter(Q(user1=user) | Q(user2=user)).order_by(
            "-created_at"
        )
    
    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["request"] = self.request
        return context


class ConversationMessagesView(generics.ListCreateAPIView):
    serializer_class = MessageSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        conv_id = self.kwargs["pk"]
        # Check if user is a participant in this conversation
        try:
            conv = Conversation.objects.get(id=conv_id)
            if conv.user1 != self.request.user and conv.user2 != self.request.user:
                return Message.objects.none()  # Return empty queryset if not a participant
        except Conversation.DoesNotExist:
            return Message.objects.none()
        
        qs = Message.objects.filter(conversation_id=conv_id).order_by("created_at")
        # Mark messages as read when fetched by the recipient
        qs.filter(is_read=False).exclude(sender=self.request.user).update(is_read=True)
        return qs

    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["request"] = self.request
        return context

    def perform_create(self, serializer):
        conv_id = self.kwargs["pk"]
        # Verify user is a participant before allowing message creation
        try:
            conv = Conversation.objects.get(id=conv_id)
            if conv.user1 != self.request.user and conv.user2 != self.request.user:
                raise PermissionDenied("You are not a participant in this conversation.")
        except Conversation.DoesNotExist:
            raise NotFound("Conversation not found.")
        
        serializer.save(
            conversation_id=conv_id,
            sender=self.request.user,
        )


class StartConversationView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, *args, **kwargs):
        other_user_id = request.data.get("user_id")
        if not other_user_id:
            return Response({"detail": "user_id is required"}, status=400)

        user = request.user
        # Ensure user1.id < user2.id for consistent unique_together constraint
        user1_id = min(user.id, int(other_user_id))
        user2_id = max(user.id, int(other_user_id))
        
        # Prevent users from creating a conversation with themselves
        if user1_id == user2_id:
            return Response({"detail": "Cannot create a conversation with yourself."}, status=400)

        conv, created = Conversation.objects.get_or_create(
            user1_id=user1_id,
            user2_id=user2_id,
        )
        return Response({"conversation_id": conv.id})
