from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List
from app.common.dependencies import get_db, get_current_user
from app.recipes import service, schemas

router = APIRouter(prefix="/recipes", tags=["🍳 Recetas"])

@router.get("/suggestions", response_model=List[schemas.RecipeSummary])
async def get_suggestions(
    limit: int = 10,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    return service.get_recipe_suggestions(db, current_user.id, limit)

@router.post("/{id}/interact", status_code=status.HTTP_201_CREATED)
async def interact_with_recipe(
    id: str,
    interaction: schemas.RecipeInteractionCreate,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    if interaction.action == "cooked":
        service.register_cook_interaction(db, current_user.id, id)
    else:
        # Lógica para 'viewed' o otros
        new_int = service.RecipeInteraction(user_id=current_user.id, recipe_id=id, action=interaction.action)
        db.add(new_int)
        db.commit()
    return {"status": "success", "message": f"Interacción '{interaction.action}' registrada."}
