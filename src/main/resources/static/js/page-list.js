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

var pageCookie = "pageControlCookie"

var pageData;

$(document).ready(function(){

    let pageCookieData = $.cookie(pageCookie);
    if (pageCookieData === undefined){
        let pageSize = $("#pageSizeSelect").find('option:selected').val();
        let sortBy = $("#sortBySelect").find('option:selected').val();
        pageCookieData = JSON.stringify({
            size: pageSize,
            sort: sortBy,
            page: 0,
            descending: false
        });
        $.cookie(pageCookie, pageCookieData, {path: '/', expires: 200})
    }
    pageData = JSON.parse($.cookie(pageCookie));
});

function savePageCookieAndReload() {
    $.cookie(pageCookie, JSON.stringify(pageData), {path: '/', expires: 200})
    window.location="admin?instance=" + encodeURIComponent(instance);
}

function pageSize(){
    pageData.size = $("#pageSizeSelect").find('option:selected').val();
    savePageCookieAndReload();
}
function pageSortBy(){
    pageData.sort=$("#sortBySelect").find('option:selected').val();
    savePageCookieAndReload();
}
function pageFirst(){
    if (pageData.page === 0){
        return;
    }
    pageData.page = 0;
    savePageCookieAndReload();
}
function pageBack(numberOfPages){
    if (pageData.page === 0){
        return;
    }
    if (pageData.page > numberOfPages - 1){
        pageData.page = numberOfPages - 1
    }
    pageData.page --;
    if (pageData.page < 0) {
        pageData.page = 0;
    }
    savePageCookieAndReload();
}
function pageForward(numberOfPages){
    if (pageData.page === numberOfPages - 1){
        return
    }
    pageData.page ++;
    if (pageData.page >= numberOfPages - 1){
        pageData.page = numberOfPages - 1
    }
    savePageCookieAndReload();
}
function pageLast(numberOfPages){
    if (pageData.page === numberOfPages -1){
        return;
    }
    pageData.page = numberOfPages - 1;
    savePageCookieAndReload();
}
function pageAscending(){
    if (pageData.descending){
        pageData.descending=false;
        savePageCookieAndReload();
    }
}
function pageDescending(){
    if (!pageData.descending){
        pageData.descending=true;
        savePageCookieAndReload();
    }
}
