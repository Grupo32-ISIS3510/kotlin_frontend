from sqlalchemy import Column, String, Integer, ForeignKey, Text, Float, DateTime, func
from sqlalchemy.orm import relationship
from app.database import Base
import uuid

class Recipe(Base):
    __tablename__ = "recipes"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    name = Column(String, nullable=False)
    description = Column(Text)
    instructions = Column(Text)
    prep_time_minutes = Column(Integer)
    servings = Column(Integer)
    category = Column(String)  # breakfast, lunch, dinner, snack
    image_url = Column(String, nullable=True)
    created_at = Column(DateTime, server_default=func.now())

    ingredients = relationship("RecipeIngredient", back_populates="recipe", cascade="all, delete-orphan")

class RecipeIngredient(Base):
    __tablename__ = "recipe_ingredients"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    recipe_id = Column(String, ForeignKey("recipes.id"))
    ingredient_name = Column(String, nullable=False)
    quantity = Column(Float)
    unit = Column(String)

    recipe = relationship("Recipe", back_populates="ingredients")

class RecipeInteraction(Base):
    __tablename__ = "recipe_interactions"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("users.id"))
    recipe_id = Column(String, ForeignKey("recipes.id"))
    action = Column(String)  # viewed, cooked
    created_at = Column(DateTime, server_default=func.now())
