// State Management
let currentStep = 1;
let currentDish = {
    dishName: "",
    ingredients: [],
    totals: { protein: 0, carbs: 0, fats: 0 }
};

let searchTimeout = null;
let tempIngredient = null;

// DOM Elements
const steps = document.querySelectorAll('.step');
const dishNameInput = document.getElementById('dish-name-input');
const startBtn = document.getElementById('start-btn');
const backTo1Btn = document.getElementById('back-to-1');
const backTo2Btn = document.getElementById('back-to-2');
const analyzeBtn = document.getElementById('analyze-btn');
const resetBtn = document.getElementById('reset-btn');
const saveDishBtn = document.getElementById('save-dish-btn');
const viewLoggedBtn = document.getElementById('view-logged-btn');
const backFromLoggedBtn = document.getElementById('back-from-logged');
const loggedDishesList = document.getElementById('logged-dishes-list');

const displayDishName = document.getElementById('display-dish-name');
const summaryDishName = document.getElementById('summary-dish-name');
const ingredientSearch = document.getElementById('ingredient-search');
const autocompleteList = document.getElementById('autocomplete-list');
const ingredientsList = document.getElementById('ingredients-list');
const summaryList = document.getElementById('summary-list');

const totalPDisp = document.getElementById('total-protein');
const totalCDisp = document.getElementById('total-carbs');
const totalFDisp = document.getElementById('total-fats');

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
        alert("Please enter a dish name first.");
        return;
    }
    currentDish.dishName = name;
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
    try {
        const res = await fetch('/api/logged-dishes');
        if (res.ok) {
            const dishes = await res.json();
            renderLoggedDishes(dishes);
        }
    } catch (err) {
        console.error("Failed to fetch logged dishes", err);
    }
}

function renderLoggedDishes(dishes) {
    loggedDishesList.innerHTML = "";
    if (!dishes || dishes.length === 0) {
        loggedDishesList.innerHTML = '<div class="empty-state">No dishes logged yet.</div>';
        return;
    }

    // Show newest first
    [...dishes].reverse().forEach(dish => {
        const div = document.createElement('div');
        div.className = 'logged-item';
        div.innerHTML = `
            <div class="logged-item-header">
                <div class="logged-item-name">${dish.name}</div>
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
        div.textContent = item;
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
        }
    } catch (err) {
        console.error("Lookup failed", err);
    }
}

function showModal(ingredient) {
    tempIngredient = ingredient;
    modalName.textContent = ingredient.name;
    modalP.textContent = ingredient.protein;
    modalC.textContent = ingredient.carbs;
    modalF.textContent = ingredient.fats;
    modalWeight.value = 100;
    modal.style.display = 'flex';
}

function closeModal() {
    modal.style.display = 'none';
    tempIngredient = null;
    ingredientSearch.value = "";
}

addIngBtn.onclick = () => {
    const weight = parseFloat(modalWeight.value);
    if (!weight || weight <= 0) return;

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
        div.innerHTML = `
            <div class="item-text">
                <div class="item-name">${ing.name}</div>
                <div class="item-macros">${ing.weight}g | P: ${ing.calculated.p.toFixed(1)} C: ${ing.calculated.c.toFixed(1)} F: ${ing.calculated.f.toFixed(1)}</div>
            </div>
            <button class="text-btn" onclick="removeIng(${index})">Remove</button>
        `;
        ingredientsList.appendChild(div);
    });
}

window.removeIng = (index) => {
    currentDish.ingredients.splice(index, 1);
    updateBuilderUI();
};

// --- Analysis & Save ---
analyzeBtn.onclick = () => {
    let tP = 0, tC = 0, tF = 0;
    summaryList.innerHTML = "";

    currentDish.ingredients.forEach(ing => {
        tP += ing.calculated.p;
        tC += ing.calculated.c;
        tF += ing.calculated.f;

        const div = document.createElement('div');
        div.className = 'ingredient-item';
        div.innerHTML = `<span>${ing.name} (${ing.weight}g)</span> <span>P: ${ing.calculated.p.toFixed(1)}g</span>`;
        summaryList.appendChild(div);
    });

    totalPDisp.textContent = tP.toFixed(2) + 'g';
    totalCDisp.textContent = tC.toFixed(2) + 'g';
    totalFDisp.textContent = tF.toFixed(2) + 'g';
    
    currentDish.totals = { protein: tP, carbs: tC, fats: tF };
    goToStep(3);
};

saveDishBtn.onclick = async () => {
    try {
        const res = await fetch('/api/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(currentDish)
        });

        if (res.ok) {
            alert("Dish saved successfully!");
            resetApp();
        } else {
            alert("Error saving dish.");
        }
    } catch (err) {
        console.error("Save failed", err);
    }
};

resetBtn.onclick = resetApp;

function resetApp() {
    currentDish = { dishName: "", ingredients: [], totals: { protein: 0, carbs: 0, fats: 0 } };
    dishNameInput.value = "";
    ingredientSearch.value = "";
    updateBuilderUI();
    goToStep(1);
}

closeModalBtn.onclick = closeModal;

function openAddNewModal(name) {
    newIngNameInput.value = name;
    newIngPInput.value = 0;
    newIngCInput.value = 0;
    newIngFInput.value = 0;
    newIngModal.style.display = 'flex';
}

function closeNewModal() {
    newIngModal.style.display = 'none';
    ingredientSearch.value = "";
}

saveNewIngBtn.onclick = async () => {
    const name = newIngNameInput.value;
    const p100 = parseFloat(newIngPInput.value) || 0;
    const c100 = parseFloat(newIngCInput.value) || 0;
    const f100 = parseFloat(newIngFInput.value) || 0;

    if (!name) return;

    // Convert to per gram
    const p = p100 / 100;
    const c = c100 / 100;
    const f = f100 / 100;

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
        } else {
            alert("Error saving ingredient.");
        }
    } catch (err) {
        console.error("Save failed", err);
    }
};

closeNewModalBtn.onclick = closeNewModal;

window.onclick = (e) => { 
    if (e.target == modal) closeModal(); 
    if (e.target == newIngModal) closeNewModal();
};
