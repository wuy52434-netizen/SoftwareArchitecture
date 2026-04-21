// 密码显示切换
function togglePassword() {
    const passwordInput = document.getElementById('password');
    const eyeIcon = document.getElementById('eye-icon');
    
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        eyeIcon.innerHTML = `
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
            <line x1="1" y1="1" x2="23" y2="23"></line>
        `;
    } else {
        passwordInput.type = 'password';
        eyeIcon.innerHTML = `
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
            <circle cx="12" cy="12" r="3"></circle>
        `;
    }
}

// 显示通知
function showNotification(message, type = 'success') {
    const notification = document.getElementById('notification');
    notification.textContent = message;
    notification.className = `notification ${type} show`;
    
    setTimeout(() => {
        notification.classList.remove('show');
    }, 3000);
}

// 快速登录
function quickLogin(userType) {
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    
    if (userType === 'admin') {
        usernameInput.value = 'admin';
        passwordInput.value = 'admin123';
        showNotification('已填入管理员账号信息', 'success');
    } else {
        usernameInput.value = 'user';
        passwordInput.value = 'user123';
        showNotification('已填入普通用户账号信息', 'success');
    }
}

// 验证登录 - 调用后端API
async function validateLogin(username, password) {
    try {
        const response = await fetch('http://127.0.0.1:5001/api/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify({ username, password })
        });
        
        const data = await response.json();
        
        if (data.ok) {
            return {
                success: true,
                user: data.user,
                redirect: data.user.role === 'admin' ? 'admin.html' : 'index.html'
            };
        } else {
            return {
                success: false,
                message: data.error || '登录失败'
            };
        }
    } catch (error) {
        console.error('登录请求失败:', error);
        return {
            success: false,
            message: '网络错误，请检查后端服务是否启动'
        };
    }
}

// 处理登录表单提交
async function handleLogin(e) {
    e.preventDefault();
    
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const remember = document.getElementById('remember').checked;
    
    if (!username || !password) {
        showNotification('请输入用户名和密码', 'error');
        return;
    }
    
    // 显示加载状态
    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<span>登录中...</span>';
    submitBtn.disabled = true;
    
    try {
        // 验证登录
        const result = await validateLogin(username, password);
        
        if (result.success) {
            showNotification(`登录成功！欢迎，${result.user.username}`, 'success');
            
            // 保存登录状态
            if (remember) {
                localStorage.setItem('rememberedUser', username);
            } else {
                localStorage.removeItem('rememberedUser');
            }
            
            // 保存当前登录用户信息
            sessionStorage.setItem('currentUser', JSON.stringify(result.user));
            
            // 延迟跳转
            setTimeout(() => {
                window.location.href = result.redirect;
            }, 1000);
        } else {
            showNotification(result.message, 'error');
            
            // 密码框抖动效果
            const passwordInput = document.getElementById('password');
            passwordInput.style.animation = 'shake 0.5s';
            setTimeout(() => {
                passwordInput.style.animation = '';
            }, 500);
        }
    } finally {
        // 恢复按钮状态
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    }
}

// 加载记住的用户名
function loadRememberedUser() {
    const rememberedUser = localStorage.getItem('rememberedUser');
    if (rememberedUser) {
        document.getElementById('username').value = rememberedUser;
        document.getElementById('remember').checked = true;
    }
}

// 检查是否已登录
function checkLoginStatus() {
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
        const user = JSON.parse(currentUser);
        // 如果已登录，可以选择直接跳转或显示提示
        // showNotification('您已登录，正在跳转...', 'success');
        // setTimeout(() => {
        //     window.location.href = user.role === 'admin' ? 'admin.html' : 'index.html';
        // }, 1000);
    }
}

// 添加抖动动画
const style = document.createElement('style');
style.textContent = `
    @keyframes shake {
        0%, 100% { transform: translateX(0); }
        10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
        20%, 40%, 60%, 80% { transform: translateX(5px); }
    }
`;
document.head.appendChild(style);

// 处理刷卡登录
function handleCardLogin() {
    const modal = document.getElementById('card-login-modal');
    const statusText = modal.querySelector('.card-status');
    
    // 显示模态框
    modal.classList.add('show');
    
    // 模拟刷卡过程
    statusText.textContent = '正在读取卡片信息...';
    
    // 模拟识别过程（3秒后显示结果）
    setTimeout(() => {
        statusText.textContent = '卡片识别成功！';
        statusText.style.color = 'var(--success-color)';
        
        // 模拟登录成功
        setTimeout(() => {
            showNotification('刷卡登录成功！', 'success');
            closeCardModal();
            
            // 这里可以添加实际的刷卡登录逻辑
            // 例如：调用后端API进行刷卡验证
            // simulateCardLogin();
        }, 1000);
    }, 2000);
}

// 关闭刷卡登录模态框
function closeCardModal() {
    const modal = document.getElementById('card-login-modal');
    const statusText = modal.querySelector('.card-status');
    
    modal.classList.remove('show');
    
    // 重置状态
    setTimeout(() => {
        statusText.textContent = '请将卡片靠近读卡器';
        statusText.style.color = '';
    }, 300);
}

// 处理刷脸登录
function handleFaceLogin() {
    const modal = document.getElementById('face-login-modal');
    const statusText = modal.querySelector('.face-status');
    
    // 显示模态框
    modal.classList.add('show');
    
    // 模拟人脸识别过程
    statusText.textContent = '正在启动摄像头...';
    
    setTimeout(() => {
        statusText.textContent = '请正对摄像头';
        
        setTimeout(() => {
            statusText.textContent = '正在识别中...';
            
            // 模拟识别过程（3秒后显示结果）
            setTimeout(() => {
                statusText.textContent = '人脸识别成功！';
                statusText.style.color = 'var(--success-color)';
                
                // 模拟登录成功
                setTimeout(() => {
                    showNotification('刷脸登录成功！', 'success');
                    closeFaceModal();
                    
                    // 这里可以添加实际的刷脸登录逻辑
                    // 例如：调用后端API进行人脸识别验证
                    // simulateFaceLogin();
                }, 1000);
            }, 2000);
        }, 1000);
    }, 500);
}

// 关闭刷脸登录模态框
function closeFaceModal() {
    const modal = document.getElementById('face-login-modal');
    const statusText = modal.querySelector('.face-status');
    
    modal.classList.remove('show');
    
    // 重置状态
    setTimeout(() => {
        statusText.textContent = '请正对摄像头进行人脸识别';
        statusText.style.color = '';
    }, 300);
}

// 点击模态框外部关闭
document.addEventListener('click', (e) => {
    const cardModal = document.getElementById('card-login-modal');
    const faceModal = document.getElementById('face-login-modal');
    
    if (e.target === cardModal) {
        closeCardModal();
    }
    
    if (e.target === faceModal) {
        closeFaceModal();
    }
});

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
    loadRememberedUser();
    checkLoginStatus();
    
    // 绑定表单提交事件
    const loginForm = document.getElementById('login-form');
    loginForm.addEventListener('submit', handleLogin);
    
    // Enter键触发登录
    document.getElementById('password').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            handleLogin(e);
        }
    });
    
    console.log('登录页面已加载');
    console.log('测试账号：');
    console.log('管理员 - 用户名: admin, 密码: admin123');
    console.log('普通用户 - 用户名: user, 密码: user123');
});
