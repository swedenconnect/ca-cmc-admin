$(document).ready(function(){
    $('#overlay-data-div').hide();
    var windowHeight = window.innerHeight;
    windowHeight = parseInt((windowHeight - 115) * 94 / 100);
    var viewHeight = windowHeight > 100 ? windowHeight : 100;
    $('#overlay-display-data-div').css("height", viewHeight).css("overflow", "auto");
});

function displayCert(serialNumber, revoked ,instance) {
    $.ajax({
        url: "getCertData",
        data: {
            serialNumber: serialNumber,
            instance: instance
        },
        success: function (result) {
            if (result != null && result.length > 0) {
                let resultJson = JSON.parse(result);
                $('#overlay-cert-html-div').html(resultJson.certHtml);
                $('#overlay-cert-pem-div').html(resultJson.pem);
                let revokeBtn = $('#cert-revoke-btn');
                let revokedLabel = $('#certificate-revoked-label');
                revokeBtn.click(function (){
                    revokeCert(serialNumber, instance);
                });
                if (revoked){
                    revokeBtn.hide();
                    revokedLabel.show();
                } else {
                    revokeBtn.show();
                    revokedLabel.hide();
                }
            } else {
                $('#overlay-display-data-div').html("No data available");
            }
            $('#overlay-data-div').show();
        }
    });

}

function viewCaChainCert(idx, instance) {
    $.ajax({
        url: "getChainCertData",
        data: {
            idx: idx,
            instance: instance
        },
        success: function (result) {
            if (result != null && result.length > 0) {
                let resultJson = JSON.parse(result);
                $('#overlay-cert-html-div').html(resultJson.certHtml);
                $('#overlay-cert-pem-div').html(resultJson.pem);
                let revokeBtn = $('#cert-revoke-btn');
                let revokedLabel = $('#certificate-revoked-label');
                revokeBtn.hide();
                revokedLabel.hide();
            } else {
                $('#overlay-display-data-div').html("No data available");
            }
            $('#overlay-data-div').show();
        }
    });
}

function revokeCert(serialNumber, instance) {
    let revokeTitle = $('<h3>').html("Revoke certificate")
    let revokeMessage = $('<span>').html("Are you sure you want to revoke this certificate.<br> This action cannot be undone!").addClass("revoke-warning");
    bootbox.dialog({
        title: revokeTitle,
        message: revokeMessage,
        closeButton: false,
        onEscape: true,
        buttons: {
            revoke: {
                label: "Revoke",
                className: "btn-danger",
                callback: function (){
                    window.location="revoke?instance=" + instance
                    + "&serialNumber=" + serialNumber
                    + "&revokeKey=" + revokeKey;
                }
            },
            cancel: {
                label: "Cancel",
                className: "btn-primary",
                callback: function (){}
            }
        }
    });
}

function setJustValidCerts(instance) {
    let justValidCerts = $("#justValidCertsInput").prop("checked");
    $.cookie("justValidCerts", justValidCerts, {expires: 200})
    window.location="admin?instance=" + instance
}

function setJustValidCerts2(value) {
    $.cookie("justValidCerts", value, {expires: 200})
    window.location="admin?instance=" + instance
}
