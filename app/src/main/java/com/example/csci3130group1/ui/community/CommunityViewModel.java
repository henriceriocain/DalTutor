package com.example.csci3130group1.ui.community;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.csci3130group1.models.CommunityThread;
import com.example.csci3130group1.models.CommunityReply;
import com.example.csci3130group1.repositories.CommunityRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class CommunityViewModel extends ViewModel {
    private final CommunityRepository repository;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentUserRole = new MutableLiveData<>("Student");
    private final MutableLiveData<String> currentUserName = new MutableLiveData<>("Unknown User");

    // Filter and sort states
    private final MutableLiveData<String> selectedSortOption = new MutableLiveData<>("Newest");
    private final MutableLiveData<String> selectedCategoryFilter = new MutableLiveData<>("All Categories");
    private final MutableLiveData<String> selectedTimeFilter = new MutableLiveData<>("All Time");

    // Search + filtering
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    // Base threads from repository before local search filtering
    private final MutableLiveData<List<CommunityThread>> baseThreads = new MutableLiveData<>(new java.util.ArrayList<>());
    // Reactive threads LiveData after applying local search filter
    private final MediatorLiveData<List<CommunityThread>> threads = new MediatorLiveData<>();
    private LiveData<List<CommunityThread>> currentThreadsSource;

    public CommunityViewModel() {
        repository = CommunityRepository.getInstance();
        loadCurrentUserInfo();
        initializeReactiveFiltering();
    }

    private void initializeReactiveFiltering() {
        // Re-query backend when sort/category/time change
        threads.addSource(selectedSortOption, this::onFilterChanged);
        threads.addSource(selectedCategoryFilter, this::onFilterChanged);
        threads.addSource(selectedTimeFilter, this::onFilterChanged);

        // Combine base list with local search filter
        threads.addSource(baseThreads, list -> applySearchFilterAndSet(list, searchQuery.getValue()));
        threads.addSource(searchQuery, q -> applySearchFilterAndSet(baseThreads.getValue(), q));

        // Initial load
        onFilterChanged(null);
    }

    private void onFilterChanged(String ignored) {
        // Remove previous source to prevent memory leaks
        if (currentThreadsSource != null) {
            threads.removeSource(currentThreadsSource);
        }
        
        // Create new source with current filter values
        currentThreadsSource = repository.getThreads(
            selectedSortOption.getValue(),
            selectedCategoryFilter.getValue(),
            selectedTimeFilter.getValue()
        );
        
        // Add new source and forward results to baseThreads for local filtering
        threads.addSource(currentThreadsSource, list -> baseThreads.setValue(list));
    }

    private void applySearchFilterAndSet(List<CommunityThread> input, String query) {
        List<CommunityThread> safe = (input != null) ? input : new java.util.ArrayList<>();
        String q = (query != null) ? query.trim().toLowerCase() : "";
        if (q.isEmpty()) {
            threads.setValue(safe);
            return;
        }
        java.util.List<CommunityThread> filtered = new java.util.ArrayList<>();
        for (CommunityThread t : safe) {
            String title = t.getTitle() != null ? t.getTitle() : "";
            if (title.toLowerCase().contains(q)) {
                filtered.add(t);
            }
        }
        threads.setValue(filtered);
    }

    // Getters for LiveData
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getCurrentUserRole() {
        return currentUserRole;
    }

    public LiveData<String> getCurrentUserName() {
        return currentUserName;
    }

    public LiveData<String> getSelectedSortOption() {
        return selectedSortOption;
    }

    public LiveData<String> getSelectedCategoryFilter() {
        return selectedCategoryFilter;
    }

    public LiveData<String> getSelectedTimeFilter() {
        return selectedTimeFilter;
    }

    // Thread operations
    public void createThread(String title, String description, String category) {
        if (title.trim().isEmpty()) {
            errorMessage.setValue("Title cannot be empty");
            return;
        }

        if (description.trim().isEmpty()) {
            errorMessage.setValue("Description cannot be empty");
            return;
        }

        if (category == null || category.equals("Select a category...")) {
            errorMessage.setValue("Please select a category");
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.setValue("User not logged in");
            return;
        }

        isLoading.setValue(true);
        
        CommunityThread thread = new CommunityThread(
            title.trim(),
            description.trim(),
            userId,
            currentUserName.getValue(),
            currentUserRole.getValue(),
            category
        );

        repository.createThread(thread, new CommunityRepository.ThreadCreationCallback() {
            @Override
            public void onSuccess(String threadId) {
                isLoading.setValue(false);
                successMessage.setValue("Thread created successfully!");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue("Failed to create thread: " + error);
            }
        });
    }

    public LiveData<List<CommunityThread>> getThreads() {
        return threads;
    }

    // Search control
    public LiveData<String> getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String q) { searchQuery.setValue(q != null ? q : ""); }

    public LiveData<List<CommunityThread>> getUserThreads() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return repository.getUserThreads(userId);
        }
        return new MutableLiveData<>();
    }

    public LiveData<List<CommunityThread>> getStarredThreads() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return repository.getStarredThreads(userId);
        }
        return new MutableLiveData<>();
    }

    public LiveData<java.util.List<com.example.csci3130group1.models.CommunityNotification>> getNotifications() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return repository.getNotifications(userId);
        }
        return new MutableLiveData<>();
    }

    public void markAllNotificationsRead() {
        String userId = getCurrentUserId();
        if (userId != null) {
            repository.markAllNotificationsRead(userId);
        }
    }

    public void markNotificationRead(String notificationId) {
        String userId = getCurrentUserId();
        if (userId != null && notificationId != null) {
            repository.markNotificationRead(userId, notificationId);
        }
    }

    public void deleteReply(String replyId, String threadId) {
        if (replyId == null || threadId == null) return;
        repository.deleteReply(replyId, threadId, new CommunityRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                successMessage.setValue("Reply deleted");
            }

            @Override
            public void onFailure(String error) {
                errorMessage.setValue("Failed to delete reply: " + error);
            }
        });
    }

    public void deleteThread(String threadId) {
        if (threadId == null) return;
        repository.deleteThread(threadId, new CommunityRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                successMessage.setValue("Thread deleted");
            }

            @Override
            public void onFailure(String error) {
                errorMessage.setValue("Failed to delete thread: " + error);
            }
        });
    }

    public LiveData<java.util.List<com.example.csci3130group1.models.CommunityReply>> getUserReplies() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return repository.getUserReplies(userId);
        }
        return new MutableLiveData<>();
    }

    // Reply operations
    public void createReply(String threadId, String content) {
        if (content.trim().isEmpty()) {
            errorMessage.setValue("Reply cannot be empty");
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.setValue("User not logged in");
            return;
        }

        isLoading.setValue(true);

        CommunityReply reply = new CommunityReply(
            threadId,
            content.trim(),
            userId,
            currentUserName.getValue(),
            currentUserRole.getValue()
        );

        repository.createReply(reply, new CommunityRepository.ReplyCreationCallback() {
            @Override
            public void onSuccess(String replyId) {
                isLoading.setValue(false);
                successMessage.setValue("Reply posted successfully!");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue("Failed to post reply: " + error);
            }
        });
    }

    public void createReply(String threadId, String content, String parentReplyId, int parentDepth) {
        if (content.trim().isEmpty()) {
            errorMessage.setValue("Reply cannot be empty");
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.setValue("User not logged in");
            return;
        }

        int maxDepth = 4; // allows 5 levels total (0..4)
        int depth = Math.min(maxDepth, Math.max(0, parentDepth + 1));
        if (depth > maxDepth) {
            errorMessage.setValue("Reached maximum reply depth");
            return;
        }

        isLoading.setValue(true);

        CommunityReply reply = new CommunityReply(
                threadId,
                content.trim(),
                userId,
                currentUserName.getValue(),
                currentUserRole.getValue(),
                parentReplyId,
                depth
        );

        repository.createReply(reply, new CommunityRepository.ReplyCreationCallback() {
            @Override
            public void onSuccess(String replyId) {
                isLoading.setValue(false);
                successMessage.setValue("Reply posted successfully!");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue("Failed to post reply: " + error);
            }
        });
    }

    public LiveData<List<CommunityReply>> getThreadReplies(String threadId) {
        return repository.getThreadReplies(threadId);
    }

    // Star operations
    public void toggleThreadStar(String threadId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.setValue("User not logged in");
            return;
        }

        repository.toggleThreadStar(threadId, userId, new CommunityRepository.StarCallback() {
            @Override
            public void onSuccess() {
                // Star toggled successfully - no need to show message
            }

            @Override
            public void onFailure(String error) {
                errorMessage.setValue("Failed to toggle star: " + error);
            }
        });
    }

    public void toggleReplyStar(String replyId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.setValue("User not logged in");
            return;
        }

        repository.toggleReplyStar(replyId, userId, new CommunityRepository.StarCallback() {
            @Override
            public void onSuccess() {
                // Star toggled successfully - no need to show message
            }

            @Override
            public void onFailure(String error) {
                errorMessage.setValue("Failed to toggle star: " + error);
            }
        });
    }

    // Filter and sort methods
    public void setSortOption(String sortOption) {
        selectedSortOption.setValue(sortOption);
    }

    public void setCategoryFilter(String categoryFilter) {
        selectedCategoryFilter.setValue(categoryFilter);
    }

    public void setTimeFilter(String timeFilter) {
        selectedTimeFilter.setValue(timeFilter);
    }

    public void clearFilters() {
        selectedSortOption.setValue("Newest");
        selectedCategoryFilter.setValue("All Categories");
        selectedTimeFilter.setValue("All Time");
    }

    // Utility methods
    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null ?
               FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    private void loadCurrentUserInfo() {
        repository.getCurrentUserRole(new CommunityRepository.UserRoleCallback() {
            @Override
            public void onSuccess(String role, String name) {
                currentUserRole.setValue(role);
                currentUserName.setValue(name);
            }

            @Override
            public void onFailure(String error) {
                currentUserRole.setValue("Student"); // Default
                currentUserName.setValue("Unknown User");
            }
        });
    }

    public void clearMessages() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
    }

    // Categories for filtering and thread creation
    public String[] getCategories() {
        return new String[]{
            "All Categories",
            "Mathematics",
            "Physics", 
            "Chemistry",
            "Biology",
            "Computer Science",
            "English",
            "History"
        };
    }

    public String[] getThreadCategories() {
        return new String[]{
            "Select a category...",
            "Mathematics",
            "Physics", 
            "Chemistry",
            "Biology",
            "Computer Science",
            "English",
            "History"
        };
    }

    public String[] getSortOptions() {
        return new String[]{"Newest", "Stars", "Replies"};
    }

    public String[] getTimeFilters() {
        return new String[]{"All Time", "Today", "This Week", "This Month"};
    }
}
