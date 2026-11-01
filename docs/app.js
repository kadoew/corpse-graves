(() => {
  const filters = Array.from(document.querySelectorAll('.filter'));
  const rows = Array.from(document.querySelectorAll('.dl-row'));
  const count = document.getElementById('dl-count');

  function applyFilter(filter) {
    let visible = 0;
    rows.forEach((row) => {
      const show = filter === 'all' || row.dataset.loader === filter;
      row.hidden = !show;
      if (show) visible += 1;
    });
    if (count) count.textContent = `${visible} ملف`;
    filters.forEach((button) => button.classList.toggle('active', button.dataset.filter === filter));
  }

  filters.forEach((button) => {
    button.addEventListener('click', () => applyFilter(button.dataset.filter || 'all'));
  });
  applyFilter('all');

  const copyButton = document.getElementById('copy-config');
  const configCode = document.getElementById('config-code');
  if (copyButton && configCode) {
    copyButton.addEventListener('click', async () => {
      const text = configCode.textContent || '';
      const previous = copyButton.textContent;
      try {
        await navigator.clipboard.writeText(text);
        copyButton.textContent = 'تم النسخ';
      } catch {
        const range = document.createRange();
        range.selectNodeContents(configCode);
        const selection = window.getSelection();
        if (selection) {
          selection.removeAllRanges();
          selection.addRange(range);
        }
        copyButton.textContent = 'حدّد النص';
      }
      window.setTimeout(() => { copyButton.textContent = previous; }, 1600);
    });
  }
})();
