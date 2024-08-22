document.addEventListener('DOMContentLoaded', function () {
    fetchNotificationsOnLoad();
    fetchUnreadNotificationCount();
    const loader = document.getElementById('loader');
    const tableContainer = document.getElementById('tableContainer');

    // Only execute the following code if the loader and tableContainer exist (i.e., we are on the correct page)
    if (loader && tableContainer) {
        loader.style.display = 'block';
        tableContainer.style.display = 'none';

        // Fetch live data on page load
        fetchLiveHostData().then(() => {
            loader.style.display = 'none';
            tableContainer.style.display = 'block';
        });

        // Set interval to refresh the data every 30 seconds without showing the loader
        setInterval(fetchLiveHostData, 30000);
    }

    // Add event listener to the form only if it exists on the page
    const addHostForm = document.getElementById('addHostForm');
    if (addHostForm) {
        addHostForm.addEventListener('submit', function (event) {
            event.preventDefault();

            const hostName = document.getElementById('hostName').value;
            const ipAddress = document.getElementById('ipAddress').value;
            const expirationDate = document.getElementById('expirationDate').value;

            fetch(`/hosts/validate-hostname?hostName=${hostName}`)
                .then(response => response.json())
                .then(data => {
                    if (data.exists) {
                        showErrorModal('Host name already exists. Please choose another one.');
                    } else if (!isValidIP(ipAddress)) {
                        showErrorModal('Invalid IP address. Please enter a valid IP.');
                    } else if (expirationDate && !isValidExpirationDate(expirationDate)){
                showErrorModal('Expiration date must be in the future.');

                    } else {
                                    showLoadingSpinner();


                    setTimeout(function () {
                                  addHostForm.submit();
                              }, 500);
                    }
                })
                .catch(error => {
                    console.error('Error validating hostname or IP:', error);
                    showErrorModal('An error occurred while validating the input.');
                });
        });
    }

    // Event listener for the edit host form
    const editHostForm = document.getElementById('editHostForm');
    if (editHostForm) {
        editHostForm.addEventListener('submit', function (event) {
            event.preventDefault();

            const ipAddress = document.getElementById('ipAddress').value;
            const expirationDate = document.getElementById('expirationDate').value;

            if (!isValidIP(ipAddress)) {
                showErrorModal('Invalid IP address. Please enter a valid IPv4 address.');
                return;
            }

            if (expirationDate && !isValidExpirationDate(expirationDate)) {
                showErrorModal('Expiration date must be in the future.');
                return;
            }

            // Show the loading spinner while processing
            showLoadingSpinner();

            // Submit the form after validation and processing
            setTimeout(function () {
                editHostForm.submit();
            }, 500);  // Simulate processing delay
        });
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
            <td><a href="hosts/service-ok/${host.hostName}">${liveInfo.serviceOk}</a></td>
            <td><a href="hosts/service-warning/${host.hostName}">${liveInfo.serviceWarning}</a></td>
            <td><a href="hosts/service-critical/${host.hostName}">${liveInfo.serviceCritical}</a></td>
        `;

        // If admin, add the action buttons
        if (isAdminUser) {
            const actionCell = document.createElement('td');
            actionCell.innerHTML = `
                 <button type="button" class="btn btn-monitor btn-sm" onclick="monitorHost('${host.id}')">Monitor</button>
                <button type="button" class="btn btn-update btn-sm" onclick="editHost('${host.id}')">Update</button>
                <button type="button" class="btn btn-delete btn-sm" onclick="confirmDeleteHost('${host.id}')">Delete</button>
            `;
            row.appendChild(actionCell);
        }

        tableBody.appendChild(row);
    });
}

function monitorHost(id) {
    // Show the loading overlay and spinner when monitoring services
    const overlay = document.getElementById('loadingOverlay');
    const statusMessage = document.getElementById('statusMessage');
    overlay.style.display = 'flex';  // Show the overlay
    statusMessage.textContent = 'Monitoring services...';

    fetch(`/hosts/monitor/${id}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        overlay.style.display = 'none';  // Hide the overlay

        if (response.ok) {
            showModalMessage('Success', 'Service discovery and monitoring initiated successfully!');
        } else {
            showModalMessage('Error', 'Unable to monitor services of this host.');
        }
    })
    .catch(error => {
        overlay.style.display = 'none';  // Hide the overlay in case of error
        console.error('Error monitoring host:', error);
        showModalMessage('Error', 'Unable to monitor services of this host.');
    });
}

// Show a modal with success or error message
function showModalMessage(title, message) {
    const modalTitle = document.getElementById('modalTitle');
    const modalBody = document.getElementById('modalBody');

    modalTitle.textContent = title;
    modalBody.textContent = message;

    const messageModal = new bootstrap.Modal(document.getElementById('messageModal'));
    messageModal.show();
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

// Function to validate IP address format
function isValidIP(ipAddress) {
    const ipPattern = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    return ipPattern.test(ipAddress);
}

// Function to validate expiration date is in the future
function isValidExpirationDate(expirationDate) {
    const today = new Date().toISOString().split('T')[0];
    return expirationDate > today;
}

// Function to show error modal
function showErrorModal(message) {
    const errorModalBody = document.getElementById('errorModalBody');
    errorModalBody.textContent = message;
    const errorModal = new bootstrap.Modal(document.getElementById('errorModal'));
    errorModal.show();
}

// Function to show loading spinner and overlay
function showLoadingSpinner() {
    const overlay = document.getElementById('loadingOverlay');
    const statusMessage = document.getElementById('statusMessage');
    overlay.style.display = 'flex';
    statusMessage.textContent = 'Processing...';
}
