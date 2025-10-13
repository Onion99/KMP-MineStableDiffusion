
### 最新报告问题

#### 无法分离与源阅读App绑定的JS代码逻辑

```js
var baseUrl = "https://www.23ddw.net/";
var tag = java.ajax(baseUrl);
if (tag) {
    var a = jsoup.parse(tag);
    push("🔥分类小说🔥", null, 1);
    for (var i = 1; i < a.length-1 ; i++) {
        var title = a[i].text();
        var url = a[i].attr("href");
        var size = 0.25;
        title = String(title).replace(/\s/g, "");
        url = String(url).replace(/_1/,'_{{page}}');      
        push(title, url, size);
    }
} else {
  java.toast("🤔列表刷新失败！！！");     
} 
```