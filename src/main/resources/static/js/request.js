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

$(document).ready(function(){

    $('#requestDataParsingResultDiv').hide();
    $('#sendRequestButton').prop('disabled', true)
    $('#certRequestInputText').bind('input propertychange', function() {
        $.ajax({
            url: "processCertReqData",
            data: {certRequestInputText: $("#certRequestInputText").val(), profile: profile},
            success: function (result) {
                handleRequestDataParserResult(result);
            }
        });
    });

});

/**
 * @param result the result from parsing the input data
 */
function handleRequestDataParserResult(result) {
    let parsingResultDiv = $('#requestDataParsingResultDiv');
    if (result === null || result === undefined){
        parsingResultDiv.html("Server connection error");
        parsingResultDiv.show();
        return;
    }
    let jsonResult = JSON.parse(result);
    let errorMessage = jsonResult.errorMessage;

    if (errorMessage != null){
        if (errorMessage === "empty"){
            parsingResultDiv.empty();
            parsingResultDiv.hide();
        } else {
            parsingResultDiv.html(errorMessage);
            parsingResultDiv.show();
        }
        $('#sendRequestButton').prop('disabled', true)
    } else {
        parsingResultDiv.empty();
        parsingResultDiv.hide();
        $('#sendRequestButton').prop('disabled', false)
    }

    // Render names from cert request input in input fields
    $.each(jsonResult.attributeValueMap, function (name, value){
        let inpId = "#"+name+"Input";
        if ($(inpId).length){
            if (value === null || value === undefined){
                $(inpId).attr('value','');
                return;
            }
            $(inpId).attr('value',value);
        }
    });
}


function strHasValue(strVal) {
    return strVal != null && strVal.length > 0;
}

function sendRequest() {

    let confirmDiv = $('<div>').addClass('cert-req-confirm-div');

    let attrTable = $('<table>').addClass('table table-striped table-sm');

    let cn;
    let country;
    // Append input field values to table
    $('#cert-req-form input:text').each(function (){
        let inputElm = $(this);
        let label = inputElm.prev().text();
        let inpId = inputElm.attr('id');
        let val = appendRow(attrTable, label, inpId);
        if (inputElm.attr('name') === 'commonName'){
            cn = val;
        }
        if (inputElm.attr('name') === 'country'){
            country = val;
        }
    });

    let countryRegex = /^[A-Z]{2}$/g
    let cnHasContent = strHasValue(cn);
    let countryHasValidContent = countryRegex.test(country);

    if (cnHasContent && countryHasValidContent){
        confirmDiv.append($('<h5>').addClass('main-color-text').html('Issue certificate to:'));
        confirmDiv.append(attrTable);

        bootbox.confirm(confirmDiv, function (e){
            if (e){
                $('#cert-req-form').submit();
            }
        });
    } else {
        confirmDiv.append('Attributes "Common name" and "Country" must have values and "Country" must have 2 characters (ISO 3166 country code)');
        bootbox.alert(confirmDiv, function (){});
    }


    function appendRow(attrTable, attrName, id) {
        let val = $('#'+id).val();
        if (strHasValue(val)){
            attrTable.append($('<tr>')
                .append($('<td>').addClass('cert-req-attr-name').html(attrName))
                .append($('<td>').html(val)));
        }
        return val;
    }

}

function ekuSettings(noEkuClick){
    let noEkuInp = $('#noEkuInput');
    let ekuInputs = $('#cert-req-form input:checkbox[id^=eku]')

    if (noEkuClick){
        noEkuInp.prop('checked', true);
        ekuInputs.each(function (){
            $(this).prop('checked', false);
        })
        return;
    }
    if (!$('#cert-req-form input:checked[id^=eku]').length){
        noEkuInp.prop('checked', true);
        return;
    }
    noEkuInp.prop('checked', false);
}


