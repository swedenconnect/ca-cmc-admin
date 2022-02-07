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
