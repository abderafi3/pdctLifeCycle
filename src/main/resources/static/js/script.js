// Function to handle the form submission for selected hosts
function debugFormSubmission() {
    const selectedHostIds = Array.from(document.querySelectorAll('input[name="selectedHostIds"]:checked')).map(cb => cb.value);
    console.log('Selected Host IDs:', selectedHostIds);
    return selectedHostIds.length > 0;
}

// Function to toggle the selection of all checkboxes
function toggleSelectAll(source) {
    const checkboxes = document.querySelectorAll('input[name="selectedHostIds"]:not(:disabled)');
    checkboxes.forEach(checkbox => checkbox.checked = source.checked);
}

// Function to redirect to edit page
function editHost(id) {
    window.location.href = '/hosts/edit/' + id;
}

// Function to show delete confirmation modal
function confirmDeleteHost(id) {
    document.getElementById('confirmDeleteButton').setAttribute('data-id', id);
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
    deleteModal.show();
}

// Function to delete a host
function deleteHost() {
    const id = document.getElementById('confirmDeleteButton').getAttribute('data-id');
    window.location.href = '/hosts/delete/' + id;
}

// Function to open the Send Notification Modal
function showNotificationModal(element) {
    const email = element.getAttribute('data-email');
    document.getElementById('userEmail').value = email;
    document.getElementById('notificationTitle').value = '';
    document.getElementById('notificationMessage').value = '';

    const sendNotificationModal = new bootstrap.Modal(document.getElementById('sendNotificationModal'));
    sendNotificationModal.show();
}

// Function to send a notification
function sendNotification() {
    const email = document.getElementById('userEmail').value;
    const title = document.getElementById('notificationTitle').value;
    const message = document.getElementById('notificationMessage').value;
    const notificationStatus = document.getElementById('notificationStatus'); // Status element

    fetch('/sendNotification', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, title, message })
    }).then(response => {
        if (response.ok) {
            notificationStatus.style.color = 'green';
            notificationStatus.textContent = 'Notification sent successfully!';
            notificationStatus.style.display = 'block'; // Show status message

            // Close the modal after 2 seconds
            setTimeout(() => {
                const sendNotificationModal = bootstrap.Modal.getInstance(document.getElementById('sendNotificationModal'));
                sendNotificationModal.hide();
                notificationStatus.style.display = 'none'; // Hide the status message after modal closes
            }, 2000);

        } else {
            notificationStatus.style.color = 'red';
            notificationStatus.textContent = 'Failed to send notification.';
            notificationStatus.style.display = 'block'; // Show status message
        }
    }).catch(error => {
        notificationStatus.style.color = 'red';
        notificationStatus.textContent = 'An error occurred while sending notification.';
        notificationStatus.style.display = 'block'; // Show status message
    });
}



document.addEventListener('DOMContentLoaded', function () {
    // Fetch notifications and count on page load
    fetchNotificationsOnLoad();
    fetchUnreadNotificationCount();
});

// Fetch unread notifications count
function fetchUnreadNotificationCount() {
    fetch('/notifications/unread-count')
        .then(response => {
            if (!response.ok) {
                throw new Error("Network response was not ok");
            }
            return response.json();
        })
        .then(count => {
            const notificationCount = document.getElementById('notification-count');
            notificationCount.textContent = count;
        })
        .catch(error => {
            console.error("Error fetching unread notification count:", error);
        });
}

// Fetch notifications for dropdown
function fetchNotificationsOnLoad() {
    fetch('/notifications')
        .then(response => response.json())
        .then(data => {
            const notificationCount = data.length;
            document.getElementById('notification-count').textContent = notificationCount;

            const notificationList = document.getElementById('notification-list');
            notificationList.innerHTML = '';

            if (notificationCount > 0) {
                data.forEach(notification => {
                    const listItem = document.createElement('li');
                    listItem.innerHTML = `<a class="dropdown-item" href="#" data-id="${notification.id}" data-title="${notification.title}" data-message="${notification.message}" onclick="showNotificationDetails(this)">${notification.title}</a>`;
                    notificationList.appendChild(listItem);
                });
            } else {
                notificationList.innerHTML = '<li><a class="dropdown-item" href="#">No new notifications</a></li>';
            }

            // Add "Show All Notifications" link
            const showAllItem = document.createElement('li');
            showAllItem.innerHTML = `<a class="dropdown-item" href="/notifications/all">Show All Notifications</a>`;
            notificationList.appendChild(showAllItem);
        })
        .catch(error => console.error('Error fetching notifications:', error));
}

// Show notification details in modal
function showNotificationDetails(element) {
    const notificationTitle = element.getAttribute('data-title');
    const notificationMessage = element.getAttribute('data-message');

    // Set modal content
    document.getElementById('notificationTitleDisplay').textContent = notificationTitle;
    document.getElementById('notificationMessageDisplay').textContent = notificationMessage;

    // Show the modal
    const notificationModal = new bootstrap.Modal(document.getElementById('viewNotificationModal'));
    notificationModal.show();

    // Mark notification as read
    const notificationId = element.getAttribute('data-id');
    markAsRead(notificationId);
}

// Mark notification as read
function markAsRead(notificationId) {
    fetch(`/notifications/read/${notificationId}`, {
        method: 'POST'
    }).then(response => {
        if (response.ok) {
            // Refresh notification count and list after marking as read
            fetchNotificationsOnLoad();
            fetchUnreadNotificationCount();
        }
    }).catch(error => {
        console.error("Error marking notification as read:", error);
    });
}


