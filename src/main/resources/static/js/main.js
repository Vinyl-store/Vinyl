document.addEventListener("DOMContentLoaded", () => {
    const destructiveForms = document.querySelectorAll("form");
    destructiveForms.forEach((form) => {
        const button = form.querySelector("button[type='submit']");
        if (!button) {
            return;
        }

        const text = button.textContent.trim().toLowerCase();
        if (text.includes("удалить") || text.includes("заблокировать") || text.includes("отмен")) {
            form.addEventListener("submit", (event) => {
                const confirmed = window.confirm("Подтвердите выполнение действия.");
                if (!confirmed) {
                    event.preventDefault();
                }
            });
        }
    });
});
