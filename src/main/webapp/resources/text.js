function saveText(text, documentId) {
    $.ajax({
        type: "put",
        data: text,
        url: contextPath + "/admin/api/document/" + documentId + "/save",
        success: function(result) {
            notify("Document Saved!");
            if (window.editor != null) window.editor.markClean();
            updateEditorButtons(false);
        },
        error: function() {
            if (xhr.status == 403) {
                location.href="/login/";
            } else {
                bootbox.alert("Could not save the changes. Sorry!");
            }
        }
    });
}

function discardText() {
    bootbox.confirm("Are you sure you want to discard all changes?", function (result) {
        if (result) {
            window.onbeforeunload = null;
            location.reload();
        }
    });
}

function updateEditorButtons(unsavedChanges) {
    //$('#save').attr('disabled', !unsavedChanges);
    $('#discard').attr('disabled', !unsavedChanges);
    $('#upload').attr('disabled', unsavedChanges);
}

function initializeEditor(editorMode) {
    var cm = CodeMirror.fromTextArea(document.getElementById("code"), {
        mode: editorMode,
        tabMode: "indent",
        lineNumbers:true,
        matchBrackets:true,
        viewportMargin:Infinity
    });
    window.editor = cm;

    updateEditorButtons(false);
    editor.on("change", function() {
        updateEditorButtons(true);
    });

    window.onbeforeunload = function (e) {
        if (!editor.isClean()) {
            return 'Your document contains unsaved changes.'
        }
    };

    return cm;
    //cm.save();
    //cm.toTextArea();
}
