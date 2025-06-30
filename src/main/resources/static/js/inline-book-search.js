/**
 * Book Club Application - Inline Book Search Module
 * Provides functionality to search for books within the book form
 */

const InlineBookSearch = (function() {

    // Default options
    const defaultOptions = {
        searchFormId: 'bookSearchForm',
        searchTitleInputId: 'bookSearchTitle',
        searchAuthorInputId: 'bookSearchAuthor',
        searchIsbnInputId: 'bookSearchIsbn',
        searchButtonId: 'executeBookSearch',
        searchResultsContainerId: 'searchResults',
        searchLoadingId: 'searchLoading',
        searchNoResultsId: 'searchNoResults',
        searchSectionId: 'searchSection',

        // Form fields
        titleInputId: 'title',
        authorInputId: 'author',
        commentsInputId: 'comments',
        fictionCheckboxId: 'fiction',
        pageCountInputId: 'pageCount',
        coverPreviewId: 'coverPreview',
        coverUrlInputId: 'coverUrl'
    };

    /**
     * Initialize inline book search functionality
     * @param {Object} customOptions - Optional. Override default element IDs
     */
    function init(customOptions = {}) {
        const options = { ...defaultOptions, ...customOptions };

        // Search form elements
        const searchForm = document.getElementById(options.searchFormId);
        const searchTitle = document.getElementById(options.searchTitleInputId);
        const searchAuthor = document.getElementById(options.searchAuthorInputId);
        const searchIsbn = document.getElementById(options.searchIsbnInputId);
        const executeSearch = document.getElementById(options.searchButtonId);
        const searchResults = document.getElementById(options.searchResultsContainerId);
        const searchLoading = document.getElementById(options.searchLoadingId);
        const searchNoResults = document.getElementById(options.searchNoResultsId);
        const searchSection = document.getElementById(options.searchSectionId);

        // Form elements
        const formElements = {
            title: document.getElementById(options.titleInputId),
            author: document.getElementById(options.authorInputId),
            comments: document.getElementById(options.commentsInputId),
            fiction: document.getElementById(options.fictionCheckboxId),
            pageCount: document.getElementById(options.pageCountInputId),
            coverPreview: document.getElementById(options.coverPreviewId),
            coverUrl: document.getElementById(options.coverUrlInputId)
        };

        // Exit if essential elements don't exist
        if (!searchForm || !searchResults) {
            console.debug('Inline book search initialization skipped: required elements not found');
            return;
        }

        // Setup clear buttons
        setupClearButtons();

        // Execute search on button click
        executeSearch.addEventListener('click', function() {
            performBookSearch();
        });

        // Also trigger search on Enter key in search input fields
        [searchTitle, searchAuthor, searchIsbn].forEach(input => {
            if (input) {
                input.addEventListener('keypress', function(e) {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        performBookSearch();
                    }
                });
            }
        });

        /**
         * Setup clear buttons for input fields
         */
        function setupClearButtons() {
            const clearButtons = document.querySelectorAll('.clear-input');
            clearButtons.forEach(button => {
                button.addEventListener('click', function() {
                    const targetId = this.getAttribute('data-target');
                    const targetInput = document.getElementById(targetId);
                    if (targetInput) {
                        targetInput.value = '';
                        targetInput.focus();
                    }
                });
            });
        }

        /**
         * Perform the book search via API
         */
        function performBookSearch() {
            // Get search terms
            const title = searchTitle ? searchTitle.value.trim() : '';
            const author = searchAuthor ? searchAuthor.value.trim() : '';
            const isbn = searchIsbn ? searchIsbn.value.trim() : '';


            // Validate that at least one search field has content
            if (!title && !author && !isbn) {
                alert('Please enter at least one search criterion');
                return;
            }

            // Build search query
            let query = '';

            if (title) query += `intitle:${title} `;
            if (author) query += `inauthor:${author} `;
            if (isbn) query += `isbn:${isbn}`;

            query = query.trim();

            // Clear previous results
            searchResults.innerHTML = '';
            searchNoResults.style.display = 'none';
            searchLoading.style.display = 'block';

            // Call our API endpoint
            fetch(`${contextPath}/api/book-search?query=${encodeURIComponent(query)}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Search request failed');
                    }
                    return response.json();
                })
                .then(books => {
                    searchLoading.style.display = 'none';

                    if (!books || books.length === 0) {
                        searchNoResults.style.display = 'block';
                        return;
                    }

                    // Create and append result items
                    books.forEach(book => {
                        const resultItem = createBookResultItem(book);
                        searchResults.appendChild(resultItem);
                    });
                })
                .catch(error => {
                    console.error('Error searching books:', error);
                    searchLoading.style.display = 'none';
                    searchNoResults.style.display = 'block';
                    searchNoResults.textContent = 'An error occurred while searching for books.';
                });
        }

        /**
         * Create a book result item element
         */
        function createBookResultItem(book) {
            const item = document.createElement('div');
            item.className = 'list-group-item list-group-item-action';

            // Book cover (if available)
            let coverHtml = '';
            if (book.coverUrl) {
                coverHtml = `
                    <div class="float-start me-3">
                        <img src="${escapeHtml(book.coverUrl)}" alt="Book cover" class="img-thumbnail" style="max-width: 100px;">
                    </div>
                `;
            }

            // Book details
            const title = book.title || 'Unknown Title';
            const author = book.author || 'Unknown Author';
            const pageCount = book.pageCount ? `<span class="badge bg-secondary">${book.pageCount} pages</span>` : '';
            const description = book.description ?
                `<p class="mb-1 small text-muted">${escapeHtml(truncateText(book.description, 200))}</p>` : '';

            // ISBN display if available
            const isbnDisplay = book.isbn ?
                `<div><small class="text-muted">ISBN: ${book.isbn}</small></div>` : '';

            item.innerHTML = `
                <div class="d-flex justify-content-between">
                    <div class="flex-grow-1">
                        ${coverHtml}
                        <h5 class="mb-1">${escapeHtml(title)}</h5>
                        <p class="mb-1"><strong>Author:</strong> ${escapeHtml(author)} ${pageCount}</p>
                        ${isbnDisplay}
                        ${description}
                    </div>
                    <div class="ms-3">
                        <button type="button" class="btn btn-primary select-book-btn">Select Book</button>
                    </div>
                </div>
            `;

            // Add event listener to select button
            item.querySelector('.select-book-btn').addEventListener('click', function() {
                selectBook(book);

                // Collapse the search section
                const searchSectionCollapse = bootstrap.Collapse.getInstance(searchSection);
                if (searchSectionCollapse) {
                    searchSectionCollapse.hide();
                }
            });

            return item;
        }

        /**
         * Select a book and fill the form
         */
        function selectBook(book) {
            // Fill form fields with book data
            if (book.title && formElements.title) formElements.title.value = book.title;
            if (book.author && formElements.author) formElements.author.value = book.author;
            if (book.description && formElements.comments) formElements.comments.value = book.description;

            // Set fiction checkbox based on book data
            if (formElements.fiction) {
                formElements.fiction.checked = book.fiction;
            }

            // Set page count if available
            if (formElements.pageCount) {
                if (book.pageCount && book.pageCount > 0) {
                    formElements.pageCount.value = book.pageCount;
                } else {
                    formElements.pageCount.value = '';
                }
            }

            // Handle cover image if available
            if (book.coverUrl && formElements.coverUrl) {
                formElements.coverUrl.value = book.coverUrl;

                // Update cover preview if available
                if (formElements.coverPreview) {
                    // Show the cover preview container
                    formElements.coverPreview.style.display = 'block';

                    // If it contains an image, update it; otherwise create one
                    let imgElement = formElements.coverPreview.querySelector('img');
                    if (!imgElement) {
                        imgElement = document.createElement('img');
                        imgElement.className = 'img-thumbnail';
                        imgElement.style.maxHeight = '200px';
                        imgElement.alt = 'Book cover preview';
                        formElements.coverPreview.innerHTML = '';
                        formElements.coverPreview.appendChild(imgElement);
                    }

                    imgElement.src = book.coverUrl;
                }
            }
        }
    }

    /**
     * Helper to truncate text
     */
    function truncateText(text, maxLength) {
        if (!text) return '';
        return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    }

    /**
     * Helper to escape HTML
     */
    function escapeHtml(unsafe) {
        if (!unsafe) return '';
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    // Public API
    return {
        init: init
    };
})();

// Automatically initialize when the DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    if (typeof InlineBookSearch !== 'undefined') {
        InlineBookSearch.init();
    }
});