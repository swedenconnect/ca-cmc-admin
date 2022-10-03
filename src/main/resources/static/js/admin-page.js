/*
 * Copyright (c) 2022.  Agency for Digital Government (DIGG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

let REASON_UNSPECIFIED = 0;
let REASON_ON_HOLD = 6;
let REASON_REMOVE_FROM_CRL = 8;

$(document).ready(function(){
    $('#overlay-data-div').hide();
    var windowHeight = window.innerHeight;
    windowHeight = parseInt((windowHeight - 115) * 94 / 100);
    var viewHeight = windowHeight > 100 ? windowHeight : 100;
    $('#overlay-display-data-div').css("height", viewHeight).css("overflow", "auto");
});

function displayCert(serialNumber, revoked, onhold ,instance) {
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
                let revokedLabel = $('#certificate-revoked-label');
                let onHoldLabel = $('#certificate-on-hold-label');
                let revokeBtn = $('#cert-revoke-btn');
                let onHoldBtn = $('#cert-on-hold-btn')
                let unrevokeBtn = $('#cert-unrevoke-btn')
                revokeBtn.click(function (){
                    revokeCert(serialNumber, instance, REASON_UNSPECIFIED);
                });
                onHoldBtn.click(function (){
                    revokeCert(serialNumber, instance, REASON_ON_HOLD);
                });
                unrevokeBtn.click(function (){
                    revokeCert(serialNumber, instance, REASON_REMOVE_FROM_CRL);
                });
                if (revoked){
                    if (onhold) {
                        revokeBtn.show();
                        unrevokeBtn.show();
                        onHoldBtn.hide();
                        onHoldLabel.show();
                        revokedLabel.hide();
                    }
                    else {
                        revokeBtn.hide();
                        onHoldBtn.hide();
                        unrevokeBtn.hide();
                        revokedLabel.show();
                        onHoldLabel.hide();
                    }
                } else {
                    revokeBtn.show();
                    onHoldBtn.show();
                    unrevokeBtn.hide();
                    onHoldLabel.hide();
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
                let onHoldLabel = $('#certificate-on-hold-label');
                let onHoldBtn = $('#cert-on-hold-btn');
                let unrevokeBtn = $('#cert-unrevoke-btn');
                revokeBtn.hide();
                revokedLabel.hide();
                onHoldBtn.hide();
                unrevokeBtn.hide();
                onHoldLabel.hide();
            } else {
                $('#overlay-display-data-div').html("No data available");
            }
            $('#overlay-data-div').show();
        }
    });
}

function revokeCert(serialNumber, instance, reason) {
    let revokeTitle = $('<h3>').html("Revoke certificate");
    let revokeMessage = $('<span>').html("Are you sure you want to revoke this certificate?<br> This action cannot be undone!").addClass("revoke-warning");
    let okButtonText = "Revoke";
    let okButtonClass = "btn-danger";
    if (reason === REASON_ON_HOLD) {
        revokeTitle = $('<h3>').html("Block certificate")
        revokeMessage = $('<span>').html("Are you sure you want to put this certificate on hold?").addClass("revoke-warning");
        okButtonText = "OK";
        okButtonClass = "btn-info";
    }
    if (reason === REASON_REMOVE_FROM_CRL) {
        revokeTitle = $('<h3>').html("Unblock certificate")
        revokeMessage = $('<span>').html("Are you sure you want to unblock this certificate?").addClass("revoke-warning");
        okButtonText = "OK";
        okButtonClass = "btn-info";
    }

    bootbox.dialog({
        title: revokeTitle,
        message: revokeMessage,
        closeButton: false,
        onEscape: true,
        buttons: {
            revoke: {
                label: okButtonText,
                className: okButtonClass,
                callback: function (){
                    window.location="revoke?instance=" + instance
                        + "&serialNumber=" + serialNumber
                        + "&revokeKey=" + revokeKey
                        + "&reason=" + reason;
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
