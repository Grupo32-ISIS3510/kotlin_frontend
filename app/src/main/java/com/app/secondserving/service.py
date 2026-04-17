from sqlalchemy.orm import Session
from sqlalchemy import func, or_
from app.recipes.models import Recipe, RecipeIngredient, RecipeInteraction
from app.inventory.models import InventoryItem  # Asumiendo ubicación estándar
from datetime import datetime, timedelta

def get_recipe_suggestions(db: Session, user_id: str, limit: int = 10):
    """
    Algoritmo Smart:
    1. Identifica ingredientes del usuario que vencen en <= 5 días.
    2. Busca recetas que contengan esos ingredientes.
    3. Puntúa y ordena por relevancia.
    """
    # 1. Obtener items próximos a vencer
    threshold_date = datetime.now() + timedelta(days=5)
    urgent_items = db.query(InventoryItem).filter(
        InventoryItem.user_id == user_id,
        InventoryItem.status == "active",
        InventoryItem.expiry_date <= threshold_date
    ).all()

    urgent_names = [item.name.lower() for item in urgent_items]

    # 2. Obtener todas las recetas para calificar (en una app real se haría vía SQL joins)
    all_recipes = db.query(Recipe).all()
    scored_recipes = []

    for recipe in all_recipes:
        matches = 0
        urgent_matches = 0

        # Verificar ingredientes de la receta contra inventario
        recipe_ingredients = [ri.ingredient_name.lower() for ri in recipe.ingredients]

        # Obtener inventario completo activo del usuario para el match general
        user_inventory = db.query(InventoryItem.name).filter(
            InventoryItem.user_id == user_id, InventoryItem.status == "active"
        ).all()
        inventory_names = [i[0].lower() for i in user_inventory]

        for ing_name in recipe_ingredients:
            if any(inv_name in ing_name or ing_name in inv_name for inv_name in inventory_names):
                matches += 1
            if any(urg_name in ing_name or ing_name in urg_name for urg_name in urgent_names):
                urgent_matches += 1

        if matches > 0:
            # Puntaje: 10 puntos por cada ingrediente urgente, 1 por cada match normal
            score = (urgent_matches * 10) + matches
            recipe.inventory_matches = matches
            scored_recipes.append((recipe, score))

    # Ordenar por puntaje descendente
    scored_recipes.sort(key=lambda x: x[1], reverse=True)

    return [r[0] for r in scored_recipes[:limit]]

def register_cook_interaction(db: Session, user_id: str, recipe_id: str):
    """
    Registra la acción 'cooked' y auto-consume ingredientes del inventario.
    """
    # Registrar interacción
    new_interaction = RecipeInteraction(user_id=user_id, recipe_id=recipe_id, action="cooked")
    db.add(new_interaction)

    # Lógica de auto-consumo (Substring match)
    recipe = db.query(Recipe).filter(Recipe.id == recipe_id).first()
    for ri in recipe.ingredients:
        # Buscar un ítem activo que coincida con el nombre del ingrediente
        item = db.query(InventoryItem).filter(
            InventoryItem.user_id == user_id,
            InventoryItem.status == "active",
            or_(InventoryItem.name.ilike(f"%{ri.ingredient_name}%"),
                func.lower(ri.ingredient_name).contains(func.lower(InventoryItem.name)))
        ).first()

        if item:
            item.status = "consumed"  # Marcar como consumido

    db.commit()
