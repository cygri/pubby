$(function() {
    init_long_literals();
    $('.expand-meta-all').click(function() {
        $('.metadata-properties').removeClass('collapsed');
        $('.expand-meta').text('hide');
        $('.expand-meta-all').addClass('muted');
        return false;
    });
    $('.expand-meta').click(function() {
        $(this).closest('.metadata-properties').toggleClass('collapsed');
        $(this).text($(this).text() == 'more' ? 'hide' : 'more');
        return false;
    });
});

var long_literal_counter = 0;
var long_literal_spans = {};
var long_literal_texts = {};
function init_long_literals() {
    var spans = document.getElementsByTagName('span');
    for (i = 0; i < spans.length; i++) {
        if (spans[i].className != 'literal') continue;
        var span = spans[i];
        var textNode = span.firstChild;
        var text = textNode.data;
        if (text.length < 300) continue;
        var match = text.match(/([^\0]{250}[^\0]*? )([^\0]*)/);
        if (!match) continue;
        span.insertBefore(document.createTextNode(match[1] + ' ... '), span.firstChild);
        span.removeChild(textNode);
        var link = document.createElement('a');
        link.href = 'javascript:expand(' + long_literal_counter + ');';
        link.appendChild(document.createTextNode('more'));
        link.className = 'expander';
        span.insertBefore(link, span.firstChild.nextSibling);
        long_literal_spans[long_literal_counter] = span;
        long_literal_texts[long_literal_counter] = textNode;
        long_literal_counter = long_literal_counter + 1;
    }
}

function expand(i) {
    var span = long_literal_spans[i];
    span.removeChild(span.firstChild);
    span.removeChild(span.firstChild);
    span.insertBefore(long_literal_texts[i], span.firstChild);
}
