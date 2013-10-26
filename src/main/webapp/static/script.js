$(function() {
    // Truncate long literals with a 'more' expander
    $('span.literal .lex').each(function(i, lex) {
        var match = $(lex).text().match(/([^\0]{250}[^\0]*? )([^\0]*)/);
        if (!match) return;
        $(lex).hide().after('<span class="lex-truncated">' + match[1] +
                ' ... <a href="#" class="expander">more</a></span>')
            .next().find('.expander').click(function() {
                $(lex).show().next().remove();
            });
    });
    // Button that expands all metadata tables
    $('.expand-meta-all').click(function() {
        $(this).addClass('muted');
        $('.metadata-properties').removeClass('collapsed')
            .find('.expand-meta').text('hide');
        return false;
    });
    // Buttons above each metadata table that toggle visibility
    $('.expand-meta').click(function() {
        $(this).text($(this).text() == 'more' ? 'hide' : 'more')
            .closest('.metadata-properties').toggleClass('collapsed');
        return false;
    });
});
