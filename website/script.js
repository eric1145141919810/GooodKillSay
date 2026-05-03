document.addEventListener('DOMContentLoaded', function() {
    const navbar = document.querySelector('.navbar');
    const downloadBtn = document.getElementById('downloadBtn');
    
    window.addEventListener('scroll', function() {
        if (window.scrollY > 50) {
            navbar.style.background = 'rgba(15, 13, 34, 0.95)';
            navbar.style.boxShadow = '0 4px 30px rgba(0, 0, 0, 0.3)';
        } else {
            navbar.style.background = 'rgba(30, 27, 75, 0.8)';
            navbar.style.boxShadow = 'none';
        }
    });
    
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                const offsetTop = target.offsetTop - 80;
                window.scrollTo({
                    top: offsetTop,
                    behavior: 'smooth'
                });
            }
        });
    });
    
    if (downloadBtn) {
        downloadBtn.addEventListener('click', function(e) {
            e.preventDefault();
            alert('请将项目目录中的 build/libs/killsay-1.0.0.jar 复制到你的 mods 文件夹！\n\n或者访问 GitHub Releases 页面下载最新版本。');
        });
    }
    
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };
    
    const observer = new IntersectionObserver(function(entries) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, observerOptions);
    
    document.querySelectorAll('.feature-card, .step, .config-card').forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(30px)';
        el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
        observer.observe(el);
    });
    
    console.log('⚔️ Goood KillSay - 官网已加载完成！');
});
