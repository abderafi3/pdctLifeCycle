document.addEventListener('DOMContentLoaded', () => {
    fetchNotificationsOnLoad();
    fetchUnreadNotificationCount();

    const loader = document.getElementById('loading');
    const tableContainer = document.getElementById('tableContainer');

    if (loader && tableContainer) {
        toggleLoader(loader, tableContainer, true);

        fetchLiveHostData().then(() => toggleLoader(loader, tableContainer, false));

        setInterval(fetchLiveHostData, 10000);
    }

    setupFormSubmit('addHostForm', handleAddHostFormSubmit);
    setupFormSubmit('editHostForm', handleEditHostFormSubmit);
});

// Toggle loader visibility
const toggleLoader = (loader, tableContainer, showLoader) => {
    loader.style.display = showLoader ? 'flex' : 'none';
    tableContainer.style.display = showLoader ? 'none' : 'block';
};

// General form submit setup function
const setupFormSubmit = (formId, submitHandler) => {
    const form = document.getElementById(formId);
    if (form) {
        form.addEventListener('submit', submitHandler);
    }
};

// Handle add host form submission
const handleAddHostFormSubmit = async (event) => {
    event.preventDefault();
    const { hostName, ipAddress, expirationDate } = getFormValues(['hostName', 'ipAddress', 'expirationDate']);

    try {
        const response = await fetch(`/hosts/validate-hostname?hostName=${hostName}`);
        const data = await response.json();

        if (data.exists) {
            showErrorModal('Host name already exists. Please choose another one.');
        } else if (!isValidIP(ipAddress)) {
            showErrorModal('Invalid IP address. Please enter a valid IP.');
        } else if (expirationDate && !isValidExpirationDate(expirationDate)) {
            showErrorModal('Expiration date must be in the future.');
        } else {
            showOverlay("Adding host");
            setTimeout(() => document.getElementById('addHostForm').submit(), 500);
        }
    } catch (error) {
        console.error('Error validating hostname or IP:', error);
        showErrorModal('An error occurred while validating the input.');
    }
};

// Handle edit host form submission
const handleEditHostFormSubmit = (event) => {
    event.preventDefault();
    const { ipAddress, expirationDate } = getFormValues(['ipAddress', 'expirationDate']);

    if (!isValidIP(ipAddress)) {
        showErrorModal('Invalid IP address. Please enter a valid IPv4 address.');
        return;
    }

    if (expirationDate && !isValidExpirationDate(expirationDate)) {
        showErrorModal('Expiration date must be in the future.');
        return;
    }

    showOverlay("Editing host");
    setTimeout(() => document.getElementById('editHostForm').submit(), 500);
};

// Fetch unread notifications count
const fetchUnreadNotificationCount = async () => {
    try {
        const response = await fetch('/notifications/unread-count');
        if (!response.ok) throw new Error('Network response was not ok');

        const count = await response.json();
        const notificationCount = document.getElementById('notification-count');
        if (notificationCount) notificationCount.textContent = count;
    } catch (error) {
        console.error('Error fetching unread notification count:', error);
    }
};

// Fetch notifications for dropdown
const fetchNotificationsOnLoad = async () => {
    try {
        const response = await fetch('/notifications');
        const data = await response.json();

        const notificationList = document.getElementById('notification-list');
        const notificationCount = document.getElementById('notification-count');

        if (notificationList && notificationCount) {
            notificationCount.textContent = data.length;
            notificationList.innerHTML = '';

            if (data.length > 0) {
                data.forEach(notification => {
                    notificationList.appendChild(createNotificationListItem(notification));
                });
            } else {
                notificationList.innerHTML = '<li><a class="dropdown-item">No new notifications</a></li>';
            }

            notificationList.appendChild(createShowAllNotificationsLink());
        }
    } catch (error) {
        console.error('Error fetching notifications:', error);
    }
};

// Create notification list item
const createNotificationListItem = (notification) => {
    const listItem = document.createElement('li');
    listItem.innerHTML = `<a class="dropdown-item" href="#" data-id="${notification.id}" data-title="${notification.title}" data-message="${notification.message}" onclick="showNotificationDetails(this)">${notification.title}</a>`;
    return listItem;
};

// Create "Show All Notifications" link
const createShowAllNotificationsLink = () => {
    const showAllItem = document.createElement('li');
    showAllItem.innerHTML = '<a class="dropdown-item" href="/notifications/all">Show All Notifications</a>';
    return showAllItem;
};

// Show notification details in modal
const showNotificationDetails = (element) => {
    const notificationTitle = element.getAttribute('data-title');
    const notificationMessage = element.getAttribute('data-message');

    const notificationTitleDisplay = document.getElementById('notificationTitleDisplay');
    const notificationMessageDisplay = document.getElementById('notificationMessageDisplay');
    if (notificationTitleDisplay && notificationMessageDisplay) {
        notificationTitleDisplay.textContent = notificationTitle;
        notificationMessageDisplay.textContent = notificationMessage;

        const notificationModal = new bootstrap.Modal(document.getElementById('viewNotificationModal'));
        notificationModal.show();

        markAsRead(element.getAttribute('data-id'));
    }
};

// Mark notification as read
const markAsRead = async (notificationId) => {
    try {
        const response = await fetch(`/notifications/read/${notificationId}`, { method: 'POST' });
        if (response.ok) {
            fetchNotificationsOnLoad();
            fetchUnreadNotificationCount();
        }
    } catch (error) {
        console.error('Error marking notification as read:', error);
    }
};

// Fetch live host data
const fetchLiveHostData = async () => {
    try {
        const response = await fetch('/hosts/live-data');
        if (!response.ok) throw new Error('Network response was not ok');

        const data = await response.json();
        updateHostsTable(data);
    } catch (error) {
        console.error('Error fetching live host data:', error);
    }
};

// Update host table
const updateHostsTable = (hosts) => {
    const tableBody = document.getElementById('hostsTableBody');
    if (!tableBody) return;

    const isAdminUser = isAdmin(); // Check if user is an admin
    tableBody.innerHTML = '';

    hosts.forEach(hostWithLiveInfo => {
        tableBody.appendChild(createHostRow(hostWithLiveInfo, isAdminUser));
    });
};

// Create host row
const createHostRow = (hostWithLiveInfo, isAdminUser) => {
    const host = hostWithLiveInfo.host;
    const liveInfo = hostWithLiveInfo.liveInfo;

    const row = document.createElement('tr');
    row.innerHTML = `
        <td>${host.hostName}</td>
        ${isAdminUser ? `<td>${host.hostUser ? `<a href="#" data-email="${host.hostUserEmail}" data-host-name="${host.hostName}" data-user-full-name="${host.hostUser}" onclick="showNotificationModal(this)">${host.hostUser}</a>` : 'None'}</td>` : ''}
        <td>${host.ipAddress}</td>
        <td>${host.creationDate}</td>
        <td>${host.expirationDate || 'None'}</td>
        <td class="${getHostStateClass(liveInfo.hostState)}">${liveInfo.hostState || 'Unknown'}</td>
        <td><a href="hosts/service-ok/${host.hostName}">${liveInfo.serviceOk}</a></td>
        <td><a href="hosts/service-warning/${host.hostName}">${liveInfo.serviceWarning}</a></td>
        <td><a href="hosts/service-critical/${host.hostName}">${liveInfo.serviceCritical}</a></td>
    `;
    if (isAdminUser) {
        row.appendChild(createActionButtons(host.id, host.ipAddress));
    }
    return row;
};


// Get host state class
const getHostStateClass = (state) => state === 'UP' ? 'status-up' : state === 'DOWN' ? 'status-down' : 'status-unknown';

// Create action buttons for admin
const createActionButtons = (hostId, hostIp) => {
    const actionCell = document.createElement('td');
    actionCell.innerHTML = `
        <button type="button" class="btn btn-update btn-sm" onclick="editHost('${hostId}')">Update</button>
        <button type="button" class="btn btn-delete btn-sm" onclick="confirmDeleteHost('${hostId}')">Delete</button>
        <button type="button" class="btn btn-agent btn-sm" onclick="openInstallAgentModal('${hostIp}')">Install Agent</button>
        <button type="button" class="btn btn-monitor btn-sm" onclick="monitorHost('${hostId}')">Monitor</button>
    `;
    return actionCell;
};



// Monitor host
const monitorHost = async (id) => {
    showOverlay('Monitoring services...');
    try {
        const response = await fetch(`/hosts/monitor/${id}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        response.ok
            ? showModalMessage('Success', 'Service discovery and monitoring initiated successfully!')
            : showModalMessage('Error', 'Service discovery on this host failed. Please verify the IP address and ensure that the Agent software is properly installed.');
    } catch (error) {
        console.error('Error monitoring host:', error);
        showModalMessage('Error', 'Unable to monitor services of this host.');
    } finally {
        hideOverlay();
    }
};

// Get form values
const getFormValues = (fields) => fields.reduce((acc, field) => {
    acc[field] = document.getElementById(field).value;
    return acc;
}, {});

// Show or hide overlay with a message
const showOverlay = (message) => {
    const overlay = document.getElementById('loadingOverlay');
    const statusMessage = document.getElementById('statusMessage');
    overlay.style.display = 'flex';
    statusMessage.textContent = message;
};

const hideOverlay = () => {
    document.getElementById('loadingOverlay').style.display = 'none';
};

// Show a modal with success or error message for host monitoring
const showModalMessage = (title, message) => {
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').textContent = message;

    const messageModal = new bootstrap.Modal(document.getElementById('messageModal'));
    messageModal.show();
};

// Show the notification modal
const showNotificationModal = (element) => {
    const email = element.getAttribute('data-email');
    const hostName = element.getAttribute('data-host-name');
    const userFullName = element.getAttribute('data-user-full-name');

    document.getElementById('userEmail').value = email;
    document.getElementById('hostName').value = hostName;
    document.getElementById('userFullName').value = userFullName;
    document.getElementById('notificationTitle').value = '';
    document.getElementById('notificationMessage').value = '';

    const sendNotificationModal = new bootstrap.Modal(document.getElementById('sendNotificationModal'));
    sendNotificationModal.show();
};


// Check if user is an admin
const isAdmin = () => {
    const userRoles = document.getElementById('userRoles');
    return userRoles?.value?.includes('ROLE_ADMIN');
};

// Function to redirect to edit page
const editHost = (id) => {
    window.location.href = `/hosts/edit/${id}`;
};

// Function to show delete confirmation modal
const confirmDeleteHost = (id) => {
    document.getElementById('confirmDeleteButton').setAttribute('data-id', id);
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
    deleteModal.show();
};

// Function to delete a host
const deleteHost = () => {
    const id = document.getElementById('confirmDeleteButton').getAttribute('data-id');
    window.location.href = `/hosts/delete/${id}`;
};

// Function to toggle the selection of all checkboxes in import.html
document.addEventListener('DOMContentLoaded', () => {
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    const importButton = document.getElementById('importButton');
    const checkboxes = document.querySelectorAll('input[name="selectedHostIds"]:not(:disabled)');

    // Run the following code only if the elements exist on the page
    if (selectAllCheckbox && importButton) {
        let checkboxArray = Array.from(checkboxes);

        // Disable select all checkbox if no hosts to select
        function updateSelectAllCheckboxState() {
            selectAllCheckbox.disabled = checkboxArray.length === 0;
        }

        // Initial check to disable "Select All" if no checkboxes
        updateSelectAllCheckboxState();

        // Event listener for individual checkboxes
        checkboxArray.forEach(checkbox => {
            checkbox.addEventListener('change', () => {
                toggleImportButton();
                updateSelectAllCheckboxState();  // Recheck to disable "Select All" if necessary
                updateSelectAllCheckbox();  // Update "Select All" state
            });
        });

        // Event listener for "Select All" checkbox
        selectAllCheckbox.addEventListener('change', toggleSelectAll);

        // Function to toggle all checkboxes when "Select All" is clicked
        function toggleSelectAll() {
            const isChecked = selectAllCheckbox.checked;
            checkboxArray.forEach(checkbox => {
                checkbox.checked = isChecked;
            });
            toggleImportButton();
        }

        // Function to enable or disable the "Import" button
        function toggleImportButton() {
            const anyChecked = checkboxArray.some(checkbox => checkbox.checked);
            importButton.disabled = !anyChecked;
        }

        // Function to update the state of the "Select All" checkbox
        function updateSelectAllCheckbox() {
            const allChecked = checkboxArray.every(checkbox => checkbox.checked);
            selectAllCheckbox.checked = allChecked;
        }

        // Initial toggle of the import button based on pre-checked boxes (if any)
        toggleImportButton();
    }
});



// Function to validate IP address format
const isValidIP = (ipAddress) => {
    const ipPattern = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    return ipPattern.test(ipAddress);
};

// Function to validate expiration date is in the future
const isValidExpirationDate = (expirationDate) => {
    const today = new Date().toISOString().split('T')[0];
    return expirationDate > today;
};

// Function to show error modal
const showErrorModal = (message) => {
    document.getElementById('errorModalBody').textContent = message;
    const errorModal = new bootstrap.Modal(document.getElementById('errorModal'));
    errorModal.show();
};


// Function to send manual notification
const sendNotification = () => {
    const email = document.getElementById('userEmail').value;
    const title = document.getElementById('notificationTitle').value;
    const message = document.getElementById('notificationMessage').value;
    const hostName = document.getElementById('hostName').value;
    const userFullName = document.getElementById('userFullName').value;
    const notificationStatus = document.getElementById('notificationStatus');

    fetch('/sendNotification', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, title, message, hostName, userFullName })
    })
    .then(response => {
        if (response.ok) {
            notificationStatus.style.color = 'green';
            notificationStatus.textContent = 'Notification sent successfully!';
            notificationStatus.style.display = 'block'; // Show status message

            // Close the modal after 2 seconds
            setTimeout(() => {
                const sendNotificationModal = bootstrap.Modal.getInstance(document.getElementById('sendNotificationModal'));
                sendNotificationModal.hide();
                notificationStatus.style.display = 'none';
            }, 1000);

        } else {
            notificationStatus.style.color = 'red';
            notificationStatus.textContent = 'Failed to send notification.';
            notificationStatus.style.display = 'block';
        }
    })
    .catch(error => {
        notificationStatus.style.color = 'red';
        notificationStatus.textContent = 'An error occurred while sending notification.';
        notificationStatus.style.display = 'block';
        console.error('Error sending notification:', error);
    });
};

const filterTable = () => {
    const filterHostName = document.getElementById('filterHostName').value.toLowerCase();
    const filterAssignedUser = document.getElementById('filterAssignedUser').value.toLowerCase();
    const rows = [...document.querySelectorAll('#notificationTable tr')];

    rows.forEach(row => {
        const hostNameCell = row.querySelectorAll('td')[5];
        const assignedUserCell = row.querySelectorAll('td')[6];

        const hostNameText = hostNameCell?.textContent.toLowerCase() || '';
        const assignedUserText = assignedUserCell?.textContent.toLowerCase() || '';

        row.style.display = (hostNameText.includes(filterHostName) && assignedUserText.includes(filterAssignedUser)) ? '' : 'none';
    });
};


// Function to open the Install Agent Modal and pass the host IP
function openInstallAgentModal(hostIp) {
    // Set the host IP in the modal's input field
    document.getElementById('hostIp').value = hostIp;

    // Reset the username, password, and status message fields
    document.getElementById('username').value = '';
    document.getElementById('password').value = '';
    document.getElementById('installStatus').style.display = 'none';

    // Open the modal
    const installAgentModal = new bootstrap.Modal(document.getElementById('installAgentModal'));
    installAgentModal.show();
}


// Function to submit the agent installation request
function submitAgentInstall() {
    const host = document.getElementById('hostIp').value;
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    // Ensure both username and password are provided
    if (!username || !password) {
        document.getElementById('installStatus').style.display = 'block';
        document.getElementById('installStatus').textContent = 'Please enter both username and password.';
        return;
    }

    // Hide the install agent modal
    const installAgentModal = bootstrap.Modal.getInstance(document.getElementById('installAgentModal'));
    installAgentModal.hide();

    // Show the overlay while processing
    showOverlay('Installing agent...');

    // Set a timeout for the fetch request
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
        controller.abort();
    }, 30000);

    // Make the API call to install the agent
    fetch(`/api/hosts/install-agent`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            host: host,
            username: username,
            password: password
        }),
        signal: controller.signal
    })
    .then(response => {
        clearTimeout(timeoutId);
        return response.text();
    })
    .then(result => {
        hideOverlay();
        showModalMessage('Agent Installation', result);
    })
    .catch(error => {
        hideOverlay();  // Hide the overlay in case of error
        if (error.name === 'AbortError') {
            showModalMessage('Agent Installation', 'Installation request timed out. Please try again.');
        } else {
            showModalMessage('Agent Installation', 'Error during agent installation: ' + error.message);
        }
    });
}







