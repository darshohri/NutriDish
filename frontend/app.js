// State Management
let currentStep = 1;
let currentDish = {
    name: "",
    ingredients: [],
    totals: { protein: 0, carbs: 0, fats: 0 }
};

let searchTimeout = null;
let tempIngredient = null;

// --- Utility: Safe text insertion (XSS prevention) ---
function escapeHTML(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// DOM Elements
const steps = document.querySelectorAll('.step');
const dishNameInput = document.getElementById('dish-name-input');
const startBtn = document.getElementById('start-btn');
const backTo1Btn = document.getElementById('back-to-1');
const backTo2Btn = document.getElementById('back-to-2');
const analyzeBtn = document.getElementById('analyze-btn');
const resetBtn = document.getElementById('reset-btn');
const saveDishBtn = document.getElementById('save-dish-btn');

const displayDishName = document.getElementById('display-dish-name');
const ingredientSearch = document.getElementById('ingredient-search');
const autocompleteList = document.getElementById('autocomplete-list');
const ingredientsList = document.getElementById('ingredients-list');

// Summary UI
const summaryDishName = document.getElementById('summary-dish-name');
const summaryList = document.getElementById('summary-list');
const totalPDisp = document.getElementById('total-protein');
const totalCDisp = document.getElementById('total-carbs');
const totalFDisp = document.getElementById('total-fats');


// Logged Dishes UI
const viewLoggedBtn = document.getElementById('view-logged-btn');
const backFromLoggedBtn = document.getElementById('back-from-logged');
const loggedDishesList = document.getElementById('logged-dishes-list');

const modal = document.getElementById('result-modal');
const modalName = document.getElementById('result-name');
const modalP = document.getElementById('result-p');
const modalC = document.getElementById('result-c');
const modalF = document.getElementById('result-f');
const modalWeight = document.getElementById('result-weight');
const addIngBtn = document.getElementById('add-ingredient-btn');
const closeModalBtn = document.getElementById('close-modal-btn');

const newIngModal = document.getElementById('new-ingredient-modal');
const newIngNameInput = document.getElementById('new-ing-name');
const newIngPInput = document.getElementById('new-ing-p');
const newIngCInput = document.getElementById('new-ing-c');
const newIngFInput = document.getElementById('new-ing-f');
const saveNewIngBtn = document.getElementById('save-new-ing-btn');
const closeNewModalBtn = document.getElementById('close-new-modal-btn');

// --- Step Logic ---
function goToStep(stepNum) {
    steps.forEach(s => s.classList.remove('active'));
    document.getElementById(`step-${stepNum}`).classList.add('active');
    currentStep = stepNum;
}

startBtn.onclick = () => {
    const name = dishNameInput.value.trim();
    if (!name) {
        showToast("Please enter a dish name first.", "error");
        return;
    }
    currentDish.name = name;
    displayDishName.textContent = name;
    summaryDishName.textContent = name;
    goToStep(2);
};

backTo1Btn.onclick = () => goToStep(1);
backTo2Btn.onclick = () => goToStep(2);

viewLoggedBtn.onclick = () => {
    fetchLoggedDishes();
    goToStep('logged');
};

backFromLoggedBtn.onclick = () => goToStep(1);

async function fetchLoggedDishes() {
    loggedDishesList.innerHTML = '<div class="empty-state">Loading dishes...</div>';
    try {
        const res = await fetch('/api/logged-dishes?t=' + Date.now());
        if (res.ok) {
            const dishes = await res.json();
            renderLoggedDishes(dishes);
        } else {
            loggedDishesList.innerHTML = '<div class="empty-state">Failed to load dishes.</div>';
        }
    } catch (err) {
        console.error("Failed to fetch logged dishes", err);
        loggedDishesList.innerHTML = '<div class="empty-state">Network error. Is the server running?</div>';
    }
}

function renderLoggedDishes(dishes) {
    loggedDishesList.innerHTML = "";
    if (!dishes || dishes.length === 0) {
        loggedDishesList.innerHTML = '<div class="empty-state">No dishes logged yet.</div>';
        return;
    }

    // Use ScrollReveal to animate dishes as they enter
    ScrollReveal().clean(loggedDishesList);

    // Show newest first
    [...dishes].reverse().forEach((dish, index) => {
        const div = document.createElement('div');
        div.className = 'logged-item';
        div.style.position = 'relative';

        // Delete Button
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'text-btn hvr-buzz';
        deleteBtn.innerHTML = '🗑️';
        deleteBtn.style.position = 'absolute';
        deleteBtn.style.top = '10px';
        deleteBtn.style.right = '10px';
        deleteBtn.style.fontSize = '1.2rem';
        deleteBtn.style.padding = '4px 8px';
        deleteBtn.style.color = '#ff4757';
        deleteBtn.onclick = () => deleteLoggedDish(dish.name);

        // Build using DOM methods for XSS safety
        const header = document.createElement('div');
        header.className = 'logged-item-header';
        const nameEl = document.createElement('div');
        nameEl.className = 'logged-item-name';
        nameEl.textContent = dish.name;
        header.appendChild(nameEl);

        const macros = document.createElement('div');
        macros.className = 'logged-item-macros';
        macros.innerHTML = `
            <div class="logged-macro">
                <span class="logged-macro-label">Protein</span>
                <span class="logged-macro-value">${dish.protein.toFixed(1)}g</span>
            </div>
            <div class="logged-macro">
                <span class="logged-macro-label">Carbs</span>
                <span class="logged-macro-value">${dish.carbs.toFixed(1)}g</span>
            </div>
            <div class="logged-macro">
                <span class="logged-macro-label">Fats</span>
                <span class="logged-macro-value">${dish.fats.toFixed(1)}g</span>
            </div>
        `;

        div.appendChild(deleteBtn);
        div.appendChild(header);
        div.appendChild(macros);
        loggedDishesList.appendChild(div);
    });

    // Apply ScrollReveal animation
    ScrollReveal().reveal('.logged-item', {
        delay: 100,
        distance: '20px',
        duration: 400,
        easing: 'ease-out',
        interval: 100,
        origin: 'bottom'
    });
}

async function deleteLoggedDish(name) {
    const isConfirmed = await confirmDeleteModal(name);
    if (!isConfirmed) return;

    try {
        let dishes = getLocalDishes();
        const initialLen = dishes.length;
        dishes = dishes.filter(d => d.name !== name);
        if (dishes.length < initialLen) {
            setLocalDishes(dishes);
            fetchLoggedDishes(); // refresh list
            showToast(`Dish '${name}' deleted successfully!`, "success");
        } else {
            showToast("Dish not found", "error");
        }
    } catch (err) {
        console.error("Delete failed", err);
        showToast("Error while trying to delete.", "error");
    }
}

// --- Autocomplete Logic ---
ingredientSearch.oninput = (e) => {
    const val = e.target.value;
    clearTimeout(searchTimeout);
    closeAutocomplete();

    if (!val || val.length < 2) return;

    searchTimeout = setTimeout(async () => {
        try {
            const res = await fetch(`/api/search?q=${encodeURIComponent(val)}`);
            const results = await res.json();
            showAutocomplete(results, val);
        } catch (err) {
            console.error("Search failed", err);
        }
    }, 300);
};

function showAutocomplete(items, query) {
    autocompleteList.innerHTML = "";
    
    items.forEach(item => {
        const div = document.createElement('div');
        div.textContent = item; // textContent is XSS-safe
        div.onclick = () => {
            lookupIngredient(item);
            closeAutocomplete();
        };
        autocompleteList.appendChild(div);
    });

    // Add "Add New" option
    const addNew = document.createElement('div');
    addNew.className = 'add-new-option';
    addNew.textContent = `+ Add "${query}" as new`;
    addNew.onclick = () => {
        openAddNewModal(query);
        closeAutocomplete();
    };
    autocompleteList.appendChild(addNew);
}

function closeAutocomplete() {
    autocompleteList.innerHTML = "";
}

// Hide autocomplete on click outside
document.addEventListener("click", (e) => {
    if (e.target !== ingredientSearch) closeAutocomplete();
});

// --- Ingredient Logic ---
async function lookupIngredient(name) {
    try {
        const res = await fetch(`/api/lookup?name=${encodeURIComponent(name)}`);
        if (res.ok) {
            const data = await res.json();
            showModal(data);
        } else {
            showToast(`Ingredient "${name}" not found in database.`, "error");
        }
    } catch (err) {
        console.error("Lookup failed", err);
        showToast("Network error during lookup. Is the server running?", "error");
    }
}

function showModal(ingredient) {
    tempIngredient = ingredient;
    modalName.textContent = ingredient.name;
    // Show per-100g values (multiply per-gram by 100)
    modalP.textContent = (ingredient.protein * 100).toFixed(1);
    modalC.textContent = (ingredient.carbs * 100).toFixed(1);
    modalF.textContent = (ingredient.fats * 100).toFixed(1);
    modalWeight.value = ''; // Empty by default
    modal.style.display = 'flex';
    
    // GSAP Modal Entry Animation (Fade in background + spring content)
    gsap.fromTo(modal, { opacity: 0 }, { opacity: 1, duration: 0.3 });
    gsap.fromTo(modal.querySelector('.modal-content'), 
        { y: 50, opacity: 0, scale: 0.9 }, 
        { y: 0, opacity: 1, scale: 1, duration: 0.4, ease: "back.out(1.7)" }
    );
}

function closeModal() {
    // GSAP Fade out
    gsap.to(modal.querySelector('.modal-content'), { y: 30, opacity: 0, scale: 0.9, duration: 0.3, ease: "power2.in" });
    gsap.to(modal, { opacity: 0, duration: 0.3, onComplete: () => {
        modal.style.display = 'none';
        tempIngredient = null;
        ingredientSearch.value = "";
    }});
}

addIngBtn.onclick = () => {
    const weight = parseFloat(modalWeight.value);
    if (!weight || weight <= 0 || weight > 50000) {
        showToast("Please enter a valid weight between 1 and 50,000 grams.", "error");
        return;
    }

    const newIng = {
        name: tempIngredient.name,
        weight: weight,
        proteinPerGram: tempIngredient.protein,
        carbsPerGram: tempIngredient.carbs,
        fatsPerGram: tempIngredient.fats,
        calculated: {
            p: tempIngredient.protein * weight,
            c: tempIngredient.carbs * weight,
            f: tempIngredient.fats * weight
        }
    };

    currentDish.ingredients.push(newIng);
    updateBuilderUI();
    closeModal();
};

function updateBuilderUI() {
    ingredientsList.innerHTML = "";
    if (currentDish.ingredients.length === 0) {
        ingredientsList.innerHTML = '<div class="empty-state">Add your first ingredient above.</div>';
        analyzeBtn.disabled = true;
        return;
    }

    analyzeBtn.disabled = false;
    currentDish.ingredients.forEach((ing, index) => {
        const div = document.createElement('div');
        div.className = 'ingredient-item';

        const textDiv = document.createElement('div');
        textDiv.className = 'item-text';

        const nameDiv = document.createElement('div');
        nameDiv.className = 'item-name';
        nameDiv.textContent = ing.name;

        const macroDiv = document.createElement('div');
        macroDiv.className = 'item-macros';
        macroDiv.textContent = `${ing.weight}g | P: ${ing.calculated.p.toFixed(1)} C: ${ing.calculated.c.toFixed(1)} F: ${ing.calculated.f.toFixed(1)}`;

        textDiv.appendChild(nameDiv);
        textDiv.appendChild(macroDiv);

        const removeBtn = document.createElement('button');
        removeBtn.className = 'text-btn';
        removeBtn.textContent = 'Remove';
        removeBtn.onclick = () => {
            currentDish.ingredients.splice(index, 1);
            updateBuilderUI();
        };

        div.appendChild(textDiv);
        div.appendChild(removeBtn);
        ingredientsList.appendChild(div);
    });
}

// --- Analysis & Save ---
analyzeBtn.onclick = () => {
    const overlay = document.getElementById('loading-overlay');
    const progressBar = document.getElementById('progress-bar');
    
    // Show overlay and reset progress
    overlay.style.display = 'flex';
    gsap.fromTo(overlay, { opacity: 0 }, { opacity: 1, duration: 0.3 });
    progressBar.style.width = '0%';
    
    // Animate progress bar to 100% over 2 seconds
    gsap.to(progressBar, { 
        width: '100%', 
        duration: 2, 
        ease: "power1.inOut",
        onComplete: () => {
            // Hide overlay instantly (goToStep has its own animation)
            overlay.style.display = 'none';
            
            // Now calculate and show summary
            document.getElementById('summary-dish-name').textContent = currentDish.name;
            summaryList.innerHTML = '';
        
            let tP = 0, tC = 0, tF = 0;
        
            currentDish.ingredients.forEach(ing => {
                tP += ing.calculated.p;
                tC += ing.calculated.c;
                tF += ing.calculated.f;
        
                const div = document.createElement('div');
                div.className = 'ingredient-item';

                const nameSpan = document.createElement('span');
                nameSpan.textContent = `${ing.name} (${ing.weight}g)`;

                const macroSpan = document.createElement('span');
                macroSpan.className = 'item-macros';
                macroSpan.textContent = `P: ${ing.calculated.p.toFixed(1)}g | C: ${ing.calculated.c.toFixed(1)}g | F: ${ing.calculated.f.toFixed(1)}g`;

                div.appendChild(nameSpan);
                div.appendChild(macroSpan);
                summaryList.appendChild(div);
            });
        
            totalPDisp.textContent = tP.toFixed(2) + 'g';
            totalCDisp.textContent = tC.toFixed(2) + 'g';
            totalFDisp.textContent = tF.toFixed(2) + 'g';
            
            currentDish.totals = { protein: tP, carbs: tC, fats: tF };
            goToStep(3);
        }
    });
};

// --- LocalStorage Helpers ---
function getLocalDishes() {
    const data = localStorage.getItem('loggedDishes');
    if (data) {
        try {
            return JSON.parse(data);
        } catch (e) {
            return [];
        }
    }
    return [];
}

function setLocalDishes(dishes) {
    localStorage.setItem('loggedDishes', JSON.stringify(dishes));
}

saveDishBtn.onclick = async () => {
    // Disable to prevent double-click
    saveDishBtn.disabled = true;
    saveDishBtn.textContent = 'Saving...';

    try {
        const dishes = getLocalDishes();
        dishes.push({
            name: currentDish.name,
            protein: currentDish.totals.protein,
            carbs: currentDish.totals.carbs,
            fats: currentDish.totals.fats
        });
        setLocalDishes(dishes);
        
        showToast("Dish saved successfully!", "success");
        resetApp();
    } catch (err) {
        console.error("Save failed", err);
        showToast("Error saving dish.", "error");
    } finally {
        saveDishBtn.disabled = false;
        saveDishBtn.textContent = 'Save to Log';
    }
};

resetBtn.onclick = resetApp;

function resetApp() {
    currentDish = { name: "", ingredients: [], totals: null };
    dishNameInput.value = "";
    ingredientSearch.value = "";
    
    // Clear lists
    ingredientsList.innerHTML = '<div class="empty-state">Add your first ingredient above.</div>';
    autocompleteList.innerHTML = '';
    
    analyzeBtn.disabled = true;
    
    // Reset save button state
    saveDishBtn.disabled = false;
    saveDishBtn.textContent = 'Save to Log';
    
    goToStep(1);
}

// --- Logged Dishes ---
viewLoggedBtn.onclick = () => {
    fetchLoggedDishes();
    goToStep('logged');
};

backFromLoggedBtn.onclick = () => {
    goToStep(currentStep === 'logged' ? 1 : currentStep);
};

async function fetchLoggedDishes() {
    loggedDishesList.innerHTML = '<div class="empty-state">Loading dishes...</div>';
    try {
        const dishes = getLocalDishes();
        renderLoggedDishes(dishes);
    } catch (err) {
        console.error("Error fetching logged dishes", err);
        loggedDishesList.innerHTML = '<div class="empty-state">Error loading dishes.</div>';
    }
}

function renderLoggedDishes(dishes) {
    loggedDishesList.innerHTML = '';
    if (dishes.length === 0) {
        loggedDishesList.innerHTML = '<div class="empty-state">No dishes logged yet.</div>';
        return;
    }

    [...dishes].reverse().forEach(dish => {
        const div = document.createElement('div');
        div.className = 'logged-item';

        div.innerHTML = `
            <div class="logged-item-header">
                <div class="logged-item-name">${dish.name}</div>
                <button class="text-btn delete-dish-btn" data-name="${dish.name}">Delete</button>
            </div>
            <div class="logged-item-macros">
                <div class="logged-macro">
                    <span class="logged-macro-label">Protein</span>
                    <span class="logged-macro-value">${dish.protein.toFixed(1)}g</span>
                </div>
                <div class="logged-macro">
                    <span class="logged-macro-label">Carbs</span>
                    <span class="logged-macro-value">${dish.carbs.toFixed(1)}g</span>
                </div>
                <div class="logged-macro">
                    <span class="logged-macro-label">Fats</span>
                    <span class="logged-macro-value">${dish.fats.toFixed(1)}g</span>
                </div>
            </div>
        `;
        loggedDishesList.appendChild(div);
    });

    // Attach delete listeners
    document.querySelectorAll('.delete-dish-btn').forEach(btn => {
        btn.onclick = async (e) => {
            const dishName = e.target.getAttribute('data-name');
            const isConfirmed = await confirmDeleteModal(dishName);
            if (isConfirmed) {
                try {
                    const res = await fetch(`/api/delete-dish?name=${encodeURIComponent(dishName)}`, { method: 'DELETE' });
                    if (res.ok) {
                        showToast("Dish deleted.", "success");
                        fetchLoggedDishes(); // refresh
                    } else {
                        showToast("Failed to delete dish.", "error");
                    }
                } catch (err) {
                    showToast("Network error.", "error");
                }
            }
        };
    });
}

function confirmDeleteModal(dishName) {
    return new Promise((resolve) => {
        const modal = document.getElementById('delete-confirm-modal');
        const text = document.getElementById('delete-confirm-text');
        const confirmBtn = document.getElementById('confirm-delete-btn');
        const cancelBtn = document.getElementById('cancel-delete-btn');
        
        text.textContent = `Are you sure you want to delete '${dishName}'?`;
        modal.style.display = 'flex';
        
        if (window.gsap) {
            gsap.fromTo(modal, { opacity: 0 }, { opacity: 1, duration: 0.3 });
            gsap.fromTo(modal.querySelector('.modal-content'), 
                { y: 50, opacity: 0, scale: 0.9 }, 
                { y: 0, opacity: 1, scale: 1, duration: 0.4, ease: "back.out(1.7)" }
            );
        }

        const close = (result) => {
            if (window.gsap) {
                gsap.to(modal.querySelector('.modal-content'), { y: 30, opacity: 0, scale: 0.9, duration: 0.3, ease: "power2.in" });
                gsap.to(modal, { opacity: 0, duration: 0.3, onComplete: () => {
                    modal.style.display = 'none';
                    resolve(result);
                }});
            } else {
                modal.style.display = 'none';
                resolve(result);
            }
        };

        confirmBtn.onclick = () => close(true);
        cancelBtn.onclick = () => close(false);
    });
}

closeModalBtn.onclick = closeModal;

function openAddNewModal(name) {
    newIngNameInput.value = name;
    newIngPInput.value = 0;
    newIngCInput.value = 0;
    newIngFInput.value = 0;
    newIngModal.style.display = 'flex';

    // GSAP Modal Entry Animation
    gsap.fromTo(newIngModal, { opacity: 0 }, { opacity: 1, duration: 0.3 });
    gsap.fromTo(newIngModal.querySelector('.modal-content'), 
        { y: 50, opacity: 0, scale: 0.9 }, 
        { y: 0, opacity: 1, scale: 1, duration: 0.4, ease: "back.out(1.7)" }
    );
}

function closeNewModal() {
    gsap.to(newIngModal.querySelector('.modal-content'), { y: 30, opacity: 0, scale: 0.9, duration: 0.3, ease: "power2.in" });
    gsap.to(newIngModal, { opacity: 0, duration: 0.3, onComplete: () => {
        newIngModal.style.display = 'none';
        ingredientSearch.value = "";
    }});
}

saveNewIngBtn.onclick = async () => {
    const name = newIngNameInput.value.trim();
    const p100 = parseFloat(newIngPInput.value) || 0;
    const c100 = parseFloat(newIngCInput.value) || 0;
    const f100 = parseFloat(newIngFInput.value) || 0;

    if (!name) {
        showToast("Ingredient name is required.", "error");
        return;
    }

    if (p100 < 0 || c100 < 0 || f100 < 0) {
        showToast("Nutritional values cannot be negative.", "error");
        return;
    }

    // Convert to per gram
    const p = p100 / 100;
    const c = c100 / 100;
    const f = f100 / 100;

    // Disable to prevent double-click
    saveNewIngBtn.disabled = true;
    saveNewIngBtn.textContent = 'Saving...';

    try {
        const res = await fetch('/api/add-ingredient', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name: name,
                proteinPerGram: p,
                carbsPerGram: c,
                fatsPerGram: f
            })
        });

        if (res.ok) {
            // Also add to current dish automatically
            const newIng = {
                name: name,
                weight: 100, // Default weight
                proteinPerGram: p,
                carbsPerGram: c,
                fatsPerGram: f,
                calculated: {
                    p: p * 100,
                    c: c * 100,
                    f: f * 100
                }
            };
            currentDish.ingredients.push(newIng);
            updateBuilderUI();
            closeNewModal();
            showToast(`Ingredient '${name}' added to database!`, "success");
        } else {
            const errData = await res.json().catch(() => ({}));
            showToast("Error saving ingredient: " + (errData.error || "Unknown error"), "error");
        }
    } catch (err) {
        console.error("Save failed", err);
        showToast("Network error. Is the server running?", "error");
    } finally {
        saveNewIngBtn.disabled = false;
        saveNewIngBtn.textContent = 'Save to Database';
    }
};

closeNewModalBtn.onclick = closeNewModal;

window.onclick = (e) => { 
    if (e.target == modal) closeModal(); 
    if (e.target == newIngModal) closeNewModal();
};

// --- Animations & Premium UI (GSAP, Lenis, ScrollReveal) ---

// 1. Lenis Smooth Scrolling
const lenis = new Lenis({
    duration: 1.2,
    easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
    direction: 'vertical',
    gestureDirection: 'vertical',
    smooth: true,
    mouseMultiplier: 1,
    smoothTouch: false,
    touchMultiplier: 2,
    infinite: false,
});

function raf(time) {
    lenis.raf(time);
    requestAnimationFrame(raf);
}
requestAnimationFrame(raf);

// 2. GSAP Entry Animations
document.addEventListener("DOMContentLoaded", () => {
    // Header animation
    gsap.from("header .logo", { y: -30, opacity: 0, duration: 0.8, ease: "bounce.out" });
    gsap.from("header h1", { x: -30, opacity: 0, duration: 0.8, delay: 0.2, ease: "power2.out" });
    gsap.from("header .subtitle", { y: 20, opacity: 0, duration: 0.8, delay: 0.4, ease: "power2.out" });

    // Step 1 Card animation
    gsap.from("#step-1 .card", { y: 40, opacity: 0, duration: 0.8, delay: 0.3, ease: "power3.out" });
});

// Override goToStep to add GSAP transitions between steps
function goToStep(stepNum) {
    // Fade out current active step
    const currentActive = document.querySelector('.step.active');
    if (currentActive) {
        gsap.to(currentActive, { 
            opacity: 0, 
            y: -20, 
            duration: 0.3, 
            onComplete: () => {
                currentActive.classList.remove('active');
                showNewStep(stepNum);
            }
        });
    } else {
        showNewStep(stepNum);
    }
}

function showNewStep(stepNum) {
    const newStep = document.getElementById(`step-${stepNum}`);
    newStep.classList.add('active');
    
    // Reset properties before animating in
    gsap.set(newStep, { opacity: 0, y: 30 });
    
    // Animate in
    gsap.to(newStep, { opacity: 1, y: 0, duration: 0.5, ease: "power3.out" });
    
    currentStep = stepNum;
}

// --- Custom Toast Notification ---
function showToast(message, type = "success") {
    const container = document.getElementById("toast-container");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    
    const icon = type === "success" ? "✅" : "❌";
    toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
    
    container.appendChild(toast);

    // Slide up and fade in
    gsap.to(toast, { opacity: 1, y: 0, duration: 0.4, ease: "back.out(1.7)" });

    // Remove after 3 seconds
    setTimeout(() => {
        gsap.to(toast, { 
            opacity: 0, 
            y: 20, 
            duration: 0.4, 
            ease: "power2.in",
            onComplete: () => toast.remove() 
        });
    }, 3000);
}
