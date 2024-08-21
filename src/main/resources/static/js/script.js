document.addEventListener('DOMContentLoaded', function () {
    fetchNotificationsOnLoad();
    fetchUnreadNotificationCount();
    const loader = document.getElementById('loader');
    const tableContainer = document.getElementById('tableContainer');

    // Only execute the following code if the loader and tableContainer exist (i.e., we are on the correct page)
    if (loader && tableContainer) {
        // Show loader initially
        loader.style.display = 'block';
        tableContainer.style.display = 'none';

        // Fetch live data on page load
        fetchLiveHostData().then(() => {
            // Hide loader and show table after the first data load
            loader.style.display = 'none';
            tableContainer.style.display = 'block';
        });

        // Set interval to refresh the data every 30 seconds without showing the loader
        setInterval(fetchLiveHostData, 30000); // 30 seconds
    }
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
            if (notificationCount) {
                notificationCount.textContent = count;
            }
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
            const notificationList = document.getElementById('notification-list');
            const notificationCount = document.getElementById('notification-count');

            if (notificationList && notificationCount) {
                const count = data.length;
                notificationCount.textContent = count;
                notificationList.innerHTML = '';

                if (count > 0) {
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
            }
        })
        .catch(error => console.error('Error fetching notifications:', error));
}

// Show notification details in modal
function showNotificationDetails(element) {
    const notificationTitle = element.getAttribute('data-title');
    const notificationMessage = element.getAttribute('data-message');

    // Set modal content
    const notificationTitleDisplay = document.getElementById('notificationTitleDisplay');
    const notificationMessageDisplay = document.getElementById('notificationMessageDisplay');
    if (notificationTitleDisplay && notificationMessageDisplay) {
        notificationTitleDisplay.textContent = notificationTitle;
        notificationMessageDisplay.textContent = notificationMessage;

        // Show the modal
        const notificationModal = new bootstrap.Modal(document.getElementById('viewNotificationModal'));
        notificationModal.show();

        // Mark notification as read
        const notificationId = element.getAttribute('data-id');
        markAsRead(notificationId);
    }
}

// Mark notification as read
function markAsRead(notificationId) {
    fetch(`/notifications/read/${notificationId}`, {
        method: 'POST'
    }).then(response => {
        if (response.ok) {
            fetchNotificationsOnLoad();
            fetchUnreadNotificationCount();
        }
    }).catch(error => {
        console.error("Error marking notification as read:", error);
    });
}

function fetchLiveHostData() {
    return fetch('/hosts/live-data')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            updateHostsTable(data);  // Call function to update the table
        })
        .catch(error => {
            console.error('Error fetching live host data:', error);
        });
}

function updateHostsTable(hosts) {
    const tableBody = document.getElementById('hostsTableBody');
    const isAdminUser = isAdmin();  // Check if user is an admin

    if (!tableBody) return;

    tableBody.innerHTML = '';

    hosts.forEach(hostWithLiveInfo => {
        const host = hostWithLiveInfo.host;
        const liveInfo = hostWithLiveInfo.liveInfo;

        const row = document.createElement('tr');

        // Create cells for host data
        row.innerHTML = `
            <td>${host.hostName}</td>
            ${isAdminUser ? `<td>${host.hostUser ? `<a href="#" data-email="${host.hostUserEmail}" onclick="showNotificationModal(this)">${host.hostUser}</a>` : 'None'}</td>` : ''}
            <td>${host.ipAddress}</td>
            <td>${host.creationDate}</td>
            <td>${host.expirationDate || 'None'}</td>
            <td class="${liveInfo.hostState === 'UP' ? 'status-up' : (liveInfo.hostState === 'DOWN' ? 'status-down' : 'status-unknown')}">
                ${liveInfo.hostState || 'Unknown'}
            </td>
            <td>${liveInfo.serviceOk}</td>
            <td>${liveInfo.serviceWarning}</td>
            <td>${liveInfo.serviceCritical}</td>
        `;

        // If admin, add the action buttons
        if (isAdminUser) {
            const actionCell = document.createElement('td');
            actionCell.innerHTML = `
                <button type="button" class="btn btn-update btn-sm" onclick="editHost('${host.id}')">Update</button>
                <button type="button" class="btn btn-delete btn-sm" onclick="confirmDeleteHost('${host.id}')">Delete</button>
            `;
            row.appendChild(actionCell);
        }

        tableBody.appendChild(row);
    });
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
                notificationStatus.style.display = 'none';
            }, 2000);

        } else {
            notificationStatus.style.color = 'red';
            notificationStatus.textContent = 'Failed to send notification.';
            notificationStatus.style.display = 'block';
        }
    }).catch(error => {
        notificationStatus.style.color = 'red';
        notificationStatus.textContent = 'An error occurred while sending notification.';
        notificationStatus.style.display = 'block';
    });
}

function isAdmin() {
    return document.getElementById('userRoles')?.value?.includes('ROLE_ADMIN');
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

// Function to toggle the selection of all checkboxes in import.html
function toggleSelectAll(source) {
    const checkboxes = document.querySelectorAll('input[name="selectedHostIds"]:not(:disabled)');
    checkboxes.forEach(checkbox => checkbox.checked = source.checked);
}

document.addEventListener('DOMContentLoaded', function () {
    const addHostForm = document.getElementById('addHostForm');

    if (addHostForm) {
        addHostForm.addEventListener('submit', function (event) {
            event.preventDefault(); // Prevent form submission until validation completes

            const hostName = document.getElementById('hostName').value;
            const ipAddress = document.getElementById('ipAddress').value;

            // Validate IP address format
            if (!isValidIpAddress(ipAddress)) {
                showErrorModal('Invalid IP address. Please enter a valid IPv4  address.');
                return;
            }

            // AJAX call to validate hostname
            fetch(`/hosts/validate-hostname?hostName=${hostName}`)
                .then(response => response.json())
                .then(data => {
                    if (data.exists) {
                        // Show error message in modal if hostname exists
                        showErrorModal('Host name already exists. Please choose another one.');
                    } else {
                        // Submit the form if validation passes
                        event.target.submit();
                    }
                })
                .catch(error => {
                    console.error('Error validating hostname:', error);
                    showErrorModal('An error occurred while validating the hostname.');
                });
        });
    }

    // Function to validate IPv4 address format
    function isValidIpAddress(ip) {
        const ipv4Pattern = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
        return ipv4Pattern.test(ip) ;
    }

    // Function to display error messages in a modal
    function showErrorModal(message) {
        const errorModalBody = document.getElementById('errorModalBody');
        errorModalBody.textContent = message;

        const errorModal = new bootstrap.Modal(document.getElementById('errorModal'));
        errorModal.show();
    }
});
