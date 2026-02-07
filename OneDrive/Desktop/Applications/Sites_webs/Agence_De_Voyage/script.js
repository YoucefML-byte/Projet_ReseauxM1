/* ========================================
   WANDERLUST TRAVEL - JAVASCRIPT
   Modern, Interactive & Accessible
======================================== */

'use strict';

/* ========================================
   VARIABLES & DOM ELEMENTS
======================================== */
const header = document.getElementById('header');
const navMenu = document.getElementById('nav-menu');
const navToggle = document.getElementById('nav-toggle');
const navClose = document.getElementById('nav-close');
const navLinks = document.querySelectorAll('.nav__link');
const themeToggle = document.getElementById('theme-toggle');
const scrollTopBtn = document.getElementById('scroll-top');
const contactForm = document.getElementById('contact-form');
const toast = document.getElementById('toast');
const toastMessage = document.getElementById('toast-message');
const toastClose = document.getElementById('toast-close');
const modal = document.getElementById('modal');
const modalOverlay = document.getElementById('modal-overlay');
const modalClose = document.getElementById('modal-close');
const modalBody = document.getElementById('modal-body');

/* ========================================
   MOBILE MENU TOGGLE
======================================== */
const showMenu = () => {
    navMenu.classList.add('show-menu');
    document.body.style.overflow = 'hidden';
};

const hideMenu = () => {
    navMenu.classList.remove('show-menu');
    document.body.style.overflow = '';
};

if (navToggle) {
    navToggle.addEventListener('click', showMenu);
}

if (navClose) {
    navClose.addEventListener('click', hideMenu);
}

// Close menu when clicking on nav links
navLinks.forEach(link => {
    link.addEventListener('click', () => {
        hideMenu();
    });
});

// Close menu when clicking outside
navMenu.addEventListener('click', (e) => {
    if (e.target === navMenu) {
        hideMenu();
    }
});

/* ========================================
   SCROLL HEADER BACKGROUND
======================================== */
const scrollHeader = () => {
    if (window.scrollY >= 50) {
        header.classList.add('scroll-header');
    } else {
        header.classList.remove('scroll-header');
    }
};

window.addEventListener('scroll', scrollHeader);

/* ========================================
   ACTIVE LINK ON SCROLL (Scroll Spy)
======================================== */
const sections = document.querySelectorAll('section[id]');

const scrollActive = () => {
    const scrollY = window.pageYOffset;

    sections.forEach(section => {
        const sectionHeight = section.offsetHeight;
        const sectionTop = section.offsetTop - 100;
        const sectionId = section.getAttribute('id');
        const link = document.querySelector(`.nav__link[href*="${sectionId}"]`);

        if (scrollY > sectionTop && scrollY <= sectionTop + sectionHeight) {
            link?.classList.add('active-link');
        } else {
            link?.classList.remove('active-link');
        }
    });
};

window.addEventListener('scroll', scrollActive);

/* ========================================
   SMOOTH SCROLL TO SECTIONS
======================================== */
navLinks.forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        const targetId = link.getAttribute('href');
        const targetSection = document.querySelector(targetId);
        
        if (targetSection) {
            const headerHeight = header.offsetHeight;
            const targetPosition = targetSection.offsetTop - headerHeight;
            
            window.scrollTo({
                top: targetPosition,
                behavior: 'smooth'
            });
        }
    });
});

// Smooth scroll for CTA buttons
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function(e) {
        const href = this.getAttribute('href');
        if (href !== '#' && href.length > 1) {
            e.preventDefault();
            const targetSection = document.querySelector(href);
            
            if (targetSection) {
                const headerHeight = header.offsetHeight;
                const targetPosition = targetSection.offsetTop - headerHeight;
                
                window.scrollTo({
                    top: targetPosition,
                    behavior: 'smooth'
                });
            }
        }
    });
});

/* ========================================
   DARK MODE TOGGLE
======================================== */
const currentTheme = localStorage.getItem('theme') || 'light';
document.documentElement.setAttribute('data-theme', currentTheme);

const toggleTheme = () => {
    const theme = document.documentElement.getAttribute('data-theme');
    const newTheme = theme === 'dark' ? 'light' : 'dark';
    
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
    
    // Add animation effect
    themeToggle.style.transform = 'rotate(360deg)';
    setTimeout(() => {
        themeToggle.style.transform = '';
    }, 300);
};

if (themeToggle) {
    themeToggle.addEventListener('click', toggleTheme);
}

/* ========================================
   SCROLL TO TOP BUTTON
======================================== */
const showScrollTop = () => {
    if (window.scrollY >= 400) {
        scrollTopBtn.classList.add('show-scroll');
    } else {
        scrollTopBtn.classList.remove('show-scroll');
    }
};

window.addEventListener('scroll', showScrollTop);

if (scrollTopBtn) {
    scrollTopBtn.addEventListener('click', () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
}

/* ========================================
   INTERSECTION OBSERVER - ANIMATIONS
======================================== */
const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('aos-animate');
        }
    });
}, observerOptions);

// Observe all elements with data-aos attribute
document.querySelectorAll('[data-aos]').forEach(element => {
    observer.observe(element);
});

/* ========================================
   TOAST NOTIFICATION SYSTEM
======================================== */
let toastTimeout;

const showToast = (message, type = 'success') => {
    toastMessage.textContent = message;
    toast.classList.add('show-toast');
    
    // Change icon based on type
    const toastIcon = toast.querySelector('.toast__icon svg');
    if (type === 'success') {
        toastIcon.innerHTML = '<polyline points="20 6 9 17 4 12"></polyline>';
        toast.querySelector('.toast__icon').style.background = 'var(--success-color)';
    } else if (type === 'error') {
        toastIcon.innerHTML = '<line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>';
        toast.querySelector('.toast__icon').style.background = 'var(--error-color)';
    }
    
    // Auto hide after 4 seconds
    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
        hideToast();
    }, 4000);
};

const hideToast = () => {
    toast.classList.remove('show-toast');
};

if (toastClose) {
    toastClose.addEventListener('click', hideToast);
}

/* ========================================
   MODAL SYSTEM
======================================== */
const showModal = (content) => {
    modalBody.innerHTML = content;
    modal.classList.add('show-modal');
    document.body.style.overflow = 'hidden';
};

const hideModal = () => {
    modal.classList.remove('show-modal');
    document.body.style.overflow = '';
};

if (modalClose) {
    modalClose.addEventListener('click', hideModal);
}

if (modalOverlay) {
    modalOverlay.addEventListener('click', hideModal);
}

// Close modal on Escape key
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal.classList.contains('show-modal')) {
        hideModal();
    }
});

/* ========================================
   DESTINATION CARDS - MODAL ON CLICK
======================================== */
const destinationCards = document.querySelectorAll('.destination-card');

destinationCards.forEach(card => {
    const btnIcon = card.querySelector('.btn-icon');
    
    if (btnIcon) {
        btnIcon.addEventListener('click', (e) => {
            e.stopPropagation();
            
            const title = card.querySelector('.destination-card__title').textContent;
            const description = card.querySelector('.destination-card__description').textContent;
            const price = card.querySelector('.price-value').textContent;
            const rating = card.querySelector('.destination-card__rating span').textContent;
            const imgSrc = card.querySelector('.destination-card__image img').src;
            
            const modalContent = `
                <div style="position: relative;">
                    <img src="${imgSrc}" alt="${title}" style="width: 100%; height: 300px; object-fit: cover; border-radius: var(--radius-lg); margin-bottom: var(--spacing-lg);">
                    <h2 style="font-size: var(--fs-3xl); margin-bottom: var(--spacing-md);">${title}</h2>
                    <div style="display: flex; align-items: center; gap: var(--spacing-md); margin-bottom: var(--spacing-lg);">
                        <div style="display: flex; align-items: center; gap: 0.25rem; color: var(--warning-color);">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                            </svg>
                            <span style="font-weight: 600;">${rating}</span>
                        </div>
                        <span style="color: var(--text-light);">|</span>
                        <span style="font-size: var(--fs-2xl); font-weight: 700; color: var(--primary-color);">${price}</span>
                    </div>
                    <p style="color: var(--text-light); line-height: 1.7; margin-bottom: var(--spacing-lg);">${description}</p>
                    <div style="background: var(--bg-secondary); padding: var(--spacing-lg); border-radius: var(--radius-md); margin-bottom: var(--spacing-lg);">
                        <h3 style="font-size: var(--fs-lg); margin-bottom: var(--spacing-md);">Ce qui est inclus :</h3>
                        <ul style="display: flex; flex-direction: column; gap: var(--spacing-sm);">
                            <li style="display: flex; align-items: center; gap: var(--spacing-sm);">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--success-color)" stroke-width="2">
                                    <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                                <span>Vol aller-retour</span>
                            </li>
                            <li style="display: flex; align-items: center; gap: var(--spacing-sm);">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--success-color)" stroke-width="2">
                                    <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                                <span>Hébergement 4 étoiles</span>
                            </li>
                            <li style="display: flex; align-items: center; gap: var(--spacing-sm);">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--success-color)" stroke-width="2">
                                    <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                                <span>Excursions guidées</span>
                            </li>
                            <li style="display: flex; align-items: center; gap: var(--spacing-sm);">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--success-color)" stroke-width="2">
                                    <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                                <span>Assurance voyage</span>
                            </li>
                        </ul>
                    </div>
                    <div style="display: flex; gap: var(--spacing-md);">
                        <button onclick="document.getElementById('modal').classList.remove('show-modal'); document.body.style.overflow = ''; document.querySelector('#contact').scrollIntoView({behavior: 'smooth'});" class="btn btn-primary" style="flex: 1;">
                            Réserver maintenant
                        </button>
                        <button onclick="showToast('Ajouté aux favoris !', 'success')" class="btn btn-secondary" style="flex: 1;">
                            Ajouter aux favoris
                        </button>
                    </div>
                </div>
            `;
            
            showModal(modalContent);
        });
    }
});

/* ========================================
   PACKAGE CARDS - CTA BUTTONS
======================================== */
const packageButtons = document.querySelectorAll('.package-card .btn');

packageButtons.forEach(button => {
    button.addEventListener('click', () => {
        const packageTitle = button.closest('.package-card').querySelector('.package-card__title').textContent;
        showToast(`Forfait "${packageTitle}" sélectionné ! Remplissez le formulaire de contact.`, 'success');
        
        // Scroll to contact form
        setTimeout(() => {
            document.querySelector('#contact').scrollIntoView({behavior: 'smooth'});
        }, 500);
    });
});

/* ========================================
   CONFIGURATOR - PRICE CALCULATOR
======================================== */
const destinationSelect = document.getElementById('destination-select');
const durationSelect = document.getElementById('duration-select');
const travelersInput = document.getElementById('travelers-input');
const insuranceCheckbox = document.getElementById('insurance');
const guideCheckbox = document.getElementById('guide');
const excursionsCheckbox = document.getElementById('excursions');
const vipTransferCheckbox = document.getElementById('vip-transfer');
const calculateBtn = document.getElementById('calculate-btn');
const requestQuoteBtn = document.getElementById('request-quote-btn');

const summaryDestination = document.getElementById('summary-destination');
const summaryDuration = document.getElementById('summary-duration');
const summaryTravelers = document.getElementById('summary-travelers');
const summaryOptions = document.getElementById('summary-options');
const totalPrice = document.getElementById('total-price');

// Calculate and update price
const calculatePrice = () => {
    // Get base price from destination
    const destinationOption = destinationSelect.options[destinationSelect.selectedIndex];
    const basePrice = parseInt(destinationOption.dataset.price);
    
    // Get duration multiplier
    const durationOption = durationSelect.options[durationSelect.selectedIndex];
    const durationMultiplier = parseFloat(durationOption.dataset.multiplier);
    
    // Get number of travelers
    const travelers = parseInt(travelersInput.value) || 1;
    
    // Calculate base total
    let total = basePrice * durationMultiplier * travelers;
    
    // Add optional extras
    const extras = [];
    
    if (insuranceCheckbox.checked) {
        const insurancePrice = parseInt(insuranceCheckbox.dataset.price);
        total += insurancePrice * travelers;
        extras.push({
            name: 'Assurance annulation',
            price: insurancePrice * travelers
        });
    }
    
    if (guideCheckbox.checked) {
        const guidePrice = parseInt(guideCheckbox.dataset.price);
        total += guidePrice;
        extras.push({
            name: 'Guide privé',
            price: guidePrice
        });
    }
    
    if (excursionsCheckbox.checked) {
        const excursionsPrice = parseInt(excursionsCheckbox.dataset.price);
        total += excursionsPrice * travelers;
        extras.push({
            name: 'Pack excursions premium',
            price: excursionsPrice * travelers
        });
    }
    
    if (vipTransferCheckbox.checked) {
        const vipPrice = parseInt(vipTransferCheckbox.dataset.price);
        total += vipPrice * travelers;
        extras.push({
            name: 'Transferts VIP',
            price: vipPrice * travelers
        });
    }
    
    // Update summary
    summaryDestination.textContent = destinationOption.textContent;
    summaryDuration.textContent = durationOption.textContent;
    summaryTravelers.textContent = `${travelers} personne${travelers > 1 ? 's' : ''}`;
    
    // Update options list
    summaryOptions.innerHTML = '';
    if (extras.length > 0) {
        extras.forEach(extra => {
            const optionEl = document.createElement('div');
            optionEl.className = 'summary-option';
            optionEl.innerHTML = `
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>${extra.name} (+${extra.price}€)</span>
            `;
            summaryOptions.appendChild(optionEl);
        });
    }
    
    // Update total price with animation
    totalPrice.style.transform = 'scale(1.1)';
    totalPrice.textContent = `${total.toLocaleString('fr-FR')}€`;
    setTimeout(() => {
        totalPrice.style.transform = '';
    }, 200);
    
    // Show success message
    showToast('Prix calculé avec succès !', 'success');
};

// Auto-calculate on change
if (destinationSelect) {
    destinationSelect.addEventListener('change', calculatePrice);
    durationSelect.addEventListener('change', calculatePrice);
    travelersInput.addEventListener('input', calculatePrice);
    insuranceCheckbox.addEventListener('change', calculatePrice);
    guideCheckbox.addEventListener('change', calculatePrice);
    excursionsCheckbox.addEventListener('change', calculatePrice);
    vipTransferCheckbox.addEventListener('change', calculatePrice);
}

// Calculate button click
if (calculateBtn) {
    calculateBtn.addEventListener('click', (e) => {
        e.preventDefault();
        calculatePrice();
    });
}

// Request quote button
if (requestQuoteBtn) {
    requestQuoteBtn.addEventListener('click', () => {
        const destination = destinationSelect.options[destinationSelect.selectedIndex].textContent;
        const duration = durationSelect.options[durationSelect.selectedIndex].textContent;
        const travelers = travelersInput.value;
        const price = totalPrice.textContent;
        
        showToast(`Demande de devis pour ${destination} envoyée ! Nous vous contacterons sous 24h.`, 'success');
        
        // Scroll to contact
        setTimeout(() => {
            document.querySelector('#contact').scrollIntoView({behavior: 'smooth'});
        }, 1000);
    });
}

/* ========================================
   CONTACT FORM - VALIDATION & SUBMIT
======================================== */
const validateEmail = (email) => {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
};

const showError = (inputId, message) => {
    const errorElement = document.getElementById(`${inputId}-error`);
    const input = document.getElementById(inputId);
    
    if (errorElement && input) {
        errorElement.textContent = message;
        input.style.borderColor = 'var(--error-color)';
        input.style.animation = 'shake 0.3s ease-in-out';
        setTimeout(() => {
            input.style.animation = '';
        }, 300);
    }
};

const clearError = (inputId) => {
    const errorElement = document.getElementById(`${inputId}-error`);
    const input = document.getElementById(inputId);
    
    if (errorElement && input) {
        errorElement.textContent = '';
        input.style.borderColor = '';
    }
};

const clearAllErrors = () => {
    ['name', 'email', 'subject', 'message', 'consent'].forEach(clearError);
};

// Real-time validation
const nameInput = document.getElementById('name');
const emailInput = document.getElementById('email');
const subjectSelect = document.getElementById('subject');
const messageTextarea = document.getElementById('message');
const consentCheckbox = document.getElementById('consent');

if (nameInput) {
    nameInput.addEventListener('blur', () => {
        if (nameInput.value.trim().length < 2) {
            showError('name', 'Le nom doit contenir au moins 2 caractères');
        } else {
            clearError('name');
        }
    });
    
    nameInput.addEventListener('input', () => {
        if (nameInput.value.trim().length >= 2) {
            clearError('name');
        }
    });
}

if (emailInput) {
    emailInput.addEventListener('blur', () => {
        if (!validateEmail(emailInput.value)) {
            showError('email', 'Veuillez entrer une adresse email valide');
        } else {
            clearError('email');
        }
    });
    
    emailInput.addEventListener('input', () => {
        if (validateEmail(emailInput.value)) {
            clearError('email');
        }
    });
}

if (subjectSelect) {
    subjectSelect.addEventListener('change', () => {
        if (subjectSelect.value) {
            clearError('subject');
        }
    });
}

if (messageTextarea) {
    messageTextarea.addEventListener('blur', () => {
        if (messageTextarea.value.trim().length < 10) {
            showError('message', 'Le message doit contenir au moins 10 caractères');
        } else {
            clearError('message');
        }
    });
    
    messageTextarea.addEventListener('input', () => {
        if (messageTextarea.value.trim().length >= 10) {
            clearError('message');
        }
    });
}

if (consentCheckbox) {
    consentCheckbox.addEventListener('change', () => {
        if (consentCheckbox.checked) {
            clearError('consent');
        }
    });
}

// Form submission
if (contactForm) {
    contactForm.addEventListener('submit', (e) => {
        e.preventDefault();
        clearAllErrors();
        
        let isValid = true;
        
        // Validate name
        if (nameInput.value.trim().length < 2) {
            showError('name', 'Le nom doit contenir au moins 2 caractères');
            isValid = false;
        }
        
        // Validate email
        if (!validateEmail(emailInput.value)) {
            showError('email', 'Veuillez entrer une adresse email valide');
            isValid = false;
        }
        
        // Validate subject
        if (!subjectSelect.value) {
            showError('subject', 'Veuillez sélectionner un sujet');
            isValid = false;
        }
        
        // Validate message
        if (messageTextarea.value.trim().length < 10) {
            showError('message', 'Le message doit contenir au moins 10 caractères');
            isValid = false;
        }
        
        // Validate consent
        if (!consentCheckbox.checked) {
            showError('consent', 'Vous devez accepter la politique de confidentialité');
            isValid = false;
        }
        
        if (isValid) {
            // Show loading state
            const submitBtn = contactForm.querySelector('button[type="submit"]');
            const originalText = submitBtn.innerHTML;
            submitBtn.disabled = true;
            submitBtn.innerHTML = `
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="animation: spin 1s linear infinite;">
                    <circle cx="12" cy="12" r="10"></circle>
                </svg>
                Envoi en cours...
            `;
            
            // Simulate API call
            setTimeout(() => {
                // Reset form
                contactForm.reset();
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
                
                // Show success message
                showToast('Message envoyé avec succès ! Nous vous répondrons sous 24h.', 'success');
                
                // Show success modal
                const successContent = `
                    <div style="text-align: center; padding: var(--spacing-lg);">
                        <div style="width: 80px; height: 80px; background: var(--success-color); color: white; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; margin: 0 auto var(--spacing-lg);">
                            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        </div>
                        <h2 style="font-size: var(--fs-3xl); margin-bottom: var(--spacing-md);">Message envoyé !</h2>
                        <p style="color: var(--text-light); font-size: var(--fs-lg); margin-bottom: var(--spacing-xl);">
                            Merci pour votre message. Notre équipe vous répondra dans les 24 heures.
                        </p>
                        <button onclick="document.getElementById('modal').classList.remove('show-modal'); document.body.style.overflow = '';" class="btn btn-primary">
                            Parfait, merci !
                        </button>
                    </div>
                `;
                
                showModal(successContent);
            }, 1500);
        } else {
            showToast('Veuillez corriger les erreurs dans le formulaire', 'error');
        }
    });
}

/* ========================================
   NEWSLETTER FORM
======================================== */
const newsletterForm = document.querySelector('.newsletter__form');

if (newsletterForm) {
    newsletterForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const emailInput = newsletterForm.querySelector('.newsletter__input');
        
        if (validateEmail(emailInput.value)) {
            showToast('Inscription à la newsletter réussie !', 'success');
            emailInput.value = '';
        } else {
            showToast('Veuillez entrer une adresse email valide', 'error');
        }
    });
}

/* ========================================
   LAZY LOADING IMAGES
======================================== */
const lazyImages = document.querySelectorAll('img[loading="lazy"]');

if ('IntersectionObserver' in window) {
    const imageObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                img.src = img.src;
                img.classList.add('loaded');
                imageObserver.unobserve(img);
            }
        });
    });
    
    lazyImages.forEach(img => imageObserver.observe(img));
}

/* ========================================
   KEYBOARD NAVIGATION ACCESSIBILITY
======================================== */
// Trap focus in modal
const focusableElements = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

modal.addEventListener('keydown', (e) => {
    if (!modal.classList.contains('show-modal')) return;
    
    const focusableContent = modal.querySelectorAll(focusableElements);
    const firstFocusable = focusableContent[0];
    const lastFocusable = focusableContent[focusableContent.length - 1];
    
    if (e.key === 'Tab') {
        if (e.shiftKey) {
            if (document.activeElement === firstFocusable) {
                lastFocusable.focus();
                e.preventDefault();
            }
        } else {
            if (document.activeElement === lastFocusable) {
                firstFocusable.focus();
                e.preventDefault();
            }
        }
    }
});

/* ========================================
   ADDITIONAL CSS ANIMATIONS
======================================== */
const style = document.createElement('style');
style.textContent = `
    @keyframes shake {
        0%, 100% { transform: translateX(0); }
        25% { transform: translateX(-10px); }
        75% { transform: translateX(10px); }
    }
    
    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }
    
    .loaded {
        animation: fadeIn 0.5s ease-in;
    }
`;
document.head.appendChild(style);

/* ========================================
   PERFORMANCE MONITORING
======================================== */
// Log performance metrics
window.addEventListener('load', () => {
    if ('performance' in window) {
        const perfData = window.performance.timing;
        const pageLoadTime = perfData.loadEventEnd - perfData.navigationStart;
        console.log(`⚡ Page loaded in ${pageLoadTime}ms`);
    }
});

/* ========================================
   INITIAL SETUP ON PAGE LOAD
======================================== */
document.addEventListener('DOMContentLoaded', () => {
    // Initialize scroll-based features
    scrollHeader();
    scrollActive();
    showScrollTop();
    
    // Calculate initial price in configurator
    if (destinationSelect) {
        calculatePrice();
    }
    
    // Add smooth entrance animations
    document.body.style.opacity = '0';
    setTimeout(() => {
        document.body.style.transition = 'opacity 0.5s ease-in';
        document.body.style.opacity = '1';
    }, 100);
    
    console.log('🌍 Wanderlust Travel website initialized successfully!');
});

/* ========================================
   ERROR HANDLING
======================================== */
window.addEventListener('error', (e) => {
    console.error('❌ Error detected:', e.message);
});

/* ========================================
   SERVICE WORKER REGISTRATION (Optional)
======================================== */
if ('serviceWorker' in navigator) {
    // Uncomment to enable service worker for offline capabilities
    // navigator.serviceWorker.register('/sw.js')
    //     .then(reg => console.log('✅ Service Worker registered'))
    //     .catch(err => console.log('❌ Service Worker registration failed:', err));
}

console.log('✅ All scripts loaded and ready!');