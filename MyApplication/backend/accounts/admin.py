from django.contrib import admin
from .models import *


@admin.register(User)
class UserAdmin(admin.ModelAdmin):
    list_display = ("seat_number", "full_name", "department", "year")
    search_fields = ("seat_number", "full_name", "university_email")


@admin.register(Profile)
class ProfileAdmin(admin.ModelAdmin):
    list_display = ("user", "course")


admin.site.register(Follow)