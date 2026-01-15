/**
 * Simple Markdown Parser for Slide Content
 * Supports: headings, bold, italic, paragraphs, lists, code
 */

class MarkdownParser {
    static parse(markdown) {
        if (!markdown || markdown.trim() === '') {
            return '<p class="preview-empty-text">No content yet</p>';
        }

        let html = markdown;

        // Escape HTML
        html = html.replace(/&/g, '&amp;')
                   .replace(/</g, '&lt;')
                   .replace(/>/g, '&gt;');

        // Process line by line
        const lines = html.split('\n');
        const processed = [];
        let inList = false;
        let listType = null;

        for (let i = 0; i < lines.length; i++) {
            let line = lines[i];

            // Headings
            if (line.startsWith('# ')) {
                processed.push(`<h1>${line.substring(2)}</h1>`);
                continue;
            }
            if (line.startsWith('## ')) {
                processed.push(`<h2>${line.substring(3)}</h2>`);
                continue;
            }
            if (line.startsWith('### ')) {
                processed.push(`<h3>${line.substring(4)}</h3>`);
                continue;
            }
            if (line.startsWith('#### ')) {
                processed.push(`<h4>${line.substring(5)}</h4>`);
                continue;
            }
            if (line.startsWith('##### ')) {
                processed.push(`<h5>${line.substring(6)}</h5>`);
                continue;
            }
            if (line.startsWith('###### ')) {
                processed.push(`<h6>${line.substring(7)}</h6>`);
                continue;
            }

            // Unordered lists
            if (line.match(/^[\*\-\+]\s+/)) {
                if (!inList || listType !== 'ul') {
                    if (inList) processed.push(`</${listType}>`);
                    processed.push('<ul>');
                    inList = true;
                    listType = 'ul';
                }
                line = line.replace(/^[\*\-\+]\s+/, '');
                processed.push(`<li>${line}</li>`);
                continue;
            }

            // Ordered lists
            if (line.match(/^\d+\.\s+/)) {
                if (!inList || listType !== 'ol') {
                    if (inList) processed.push(`</${listType}>`);
                    processed.push('<ol>');
                    inList = true;
                    listType = 'ol';
                }
                line = line.replace(/^\d+\.\s+/, '');
                processed.push(`<li>${line}</li>`);
                continue;
            }

            // Close list if we're not in a list item
            if (inList) {
                processed.push(`</${listType}>`);
                inList = false;
                listType = null;
            }

            // Blockquotes
            if (line.startsWith('> ')) {
                processed.push(`<blockquote>${line.substring(2)}</blockquote>`);
                continue;
            }

            // Horizontal rule
            if (line.match(/^(\*\*\*|---|___)\s*$/)) {
                processed.push('<hr>');
                continue;
            }

            // Empty lines
            if (line.trim() === '') {
                processed.push('<br>');
                continue;
            }

            // Regular paragraphs
            processed.push(`<p>${line}</p>`);
        }

        // Close any open lists
        if (inList) {
            processed.push(`</${listType}>`);
        }

        html = processed.join('\n');

        // Inline formatting (after block processing)

        // Code blocks
        html = html.replace(/```([^`]+)```/g, '<pre><code>$1</code></pre>');

        // Inline code
        html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

        // Bold
        html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/__([^_]+)__/g, '<strong>$1</strong>');

        // Italic
        html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        html = html.replace(/_([^_]+)_/g, '<em>$1</em>');

        // Links (basic support)
        html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');

        return html;
    }

    static stripMarkdown(markdown) {
        return markdown
            .replace(/^#+\s+/gm, '') // Remove headings
            .replace(/\*\*([^*]+)\*\*/g, '$1') // Remove bold
            .replace(/\*([^*]+)\*/g, '$1') // Remove italic
            .replace(/__([^_]+)__/g, '$1')
            .replace(/_([^_]+)_/g, '$1')
            .replace(/`([^`]+)`/g, '$1') // Remove code
            .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1') // Remove links
            .trim();
    }
}
