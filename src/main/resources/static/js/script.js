function debugFormSubmission() {
    const selectedHostIds = Array.from(document.querySelectorAll('input[name="selectedHostIds"]:checked')).map(cb => cb.value);
    console.log('Selected Host IDs:', selectedHostIds);
    return selectedHostIds.length > 0;
}

function toggleSelectAll(source) {
    const checkboxes = document.querySelectorAll('input[name="selectedHostIds"]:not(:disabled)');
    for (let i = 0; i < checkboxes.length; i++) {
        checkboxes[i].checked = source.checked;
    }
}

// Host Deletion Modal

function editHost(id) {
    window.location.href = '/hosts/edit/' + id;
}

function confirmDeleteHost(id) {
    document.getElementById('confirmDeleteButton').setAttribute('data-id', id);
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
    deleteModal.show();
}

function deleteHost() {
    const id = document.getElementById('confirmDeleteButton').getAttribute('data-id');
    window.location.href = '/hosts/delete/' + id;
}


// Notification Modal

function showNotificationModal(element) {
    const email = element.getAttribute('data-email');

    document.getElementById('userEmail').value = email;
    document.getElementById('notificationTitle').value = '';
    document.getElementById('notificationMessage').value = '';

    const notificationModal = new bootstrap.Modal(document.getElementById('notificationModal'), {});
    notificationModal.show();
}

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
            const notificationModal = bootstrap.Modal.getInstance(document.getElementById('notificationModal'));
            notificationModal.hide();
        } else {
            alert('Failed to send notification.');
        }
    });
}


function showNotificationDetails(element) {
    const notificationId = element.getAttribute('data-id');
    const notificationTitle = element.innerText;

    // Simulate fetching notification details (in reality, you might need to fetch this from the server)
    const notificationMessage = "This is the detailed message for the notification: " + notificationTitle;

    // Set the modal content
    document.getElementById('notificationTitle').innerText = notificationTitle;
    document.getElementById('notificationMessage').innerText = notificationMessage;

    // Show the modal
    const notificationModal = new bootstrap.Modal(document.getElementById('notificationModal'));
    notificationModal.show();

    // Mark the notification as read
    markAsRead(notificationId);
}

function markAsRead(notificationId) {
    fetch('/notifications/read/' + notificationId, {
        method: 'POST'
    }).then(response => {
        if (response.ok) {
            // Optionally, refresh the notification list or remove the read notification
            location.reload(); // Reload the page to reflect the changes
        }
    });
}


