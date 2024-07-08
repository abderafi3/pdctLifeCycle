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



function editHost(id) {
    window.location.href = '/hosts/edit/' + id;
}

function confirmDeleteHost(id) {
    $('#confirmDeleteButton').data('id', id);
    $('#deleteConfirmModal').modal('show');
}

function deleteHost() {
    var id = $('#confirmDeleteButton').data('id');
    window.location.href = '/hosts/delete/' + id;
}
