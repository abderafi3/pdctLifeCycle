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
