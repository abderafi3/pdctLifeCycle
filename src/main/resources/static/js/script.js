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

    fetch('/sendNotification', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, title, message })
    }).then(response => {
        if (response.ok) {
            alert('Notification sent successfully!');
            const sendNotificationModal = bootstrap.Modal.getInstance(document.getElementById('sendNotificationModal'));
            sendNotificationModal.hide();
        } else {
            alert('Failed to send notification.');
        }
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

// Fetch notifications on page load and update dropdown
function fetchNotificationsOnLoad() {
    fetch('/notifications')
        .then(response => {
            if (!response.ok) {
                throw new Error("Network response was not ok");
            }
            return response.json();
        })
        .then(notifications => {
            updateNotificationsDropdown(notifications);
        })
        .catch(error => {
            console.error("Error fetching notifications:", error);
        });
}

function updateNotificationsDropdown(notifications) {
    const notificationList = document.getElementById('notification-list');

    // Clear existing notifications in the dropdown
    notificationList.innerHTML = '';

    if (notifications.length > 0) {
        notifications.forEach(notification => {
            const notificationItem = document.createElement('li');
            notificationItem.classList.add("dropdown-item");
            notificationItem.innerHTML = `
                <a href="#"
                   data-id="${notification.id}"
                   data-title="${notification.title}"
                   data-message="${notification.message}"
                   onclick="showNotificationDetails(this)">
                   ${notification.title}
                </a>`;
            notificationList.appendChild(notificationItem);
        });
    } else {
        const noNotificationsItem = document.createElement('li');
        noNotificationsItem.classList.add("dropdown-item");
        noNotificationsItem.innerHTML = '<a class="dropdown-item" href="#">No new notifications</a>';
        notificationList.appendChild(noNotificationsItem);
    }
}


// Function to display notification details in a modal
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

// Function to mark a notification as read
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


