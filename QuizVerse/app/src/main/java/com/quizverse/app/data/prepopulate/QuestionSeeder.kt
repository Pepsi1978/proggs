package com.quizverse.app.data.prepopulate

import com.quizverse.app.data.database.QuizDatabase
import com.quizverse.app.data.database.entities.Achievement
import com.quizverse.app.data.database.entities.Category
import com.quizverse.app.data.database.entities.UserProgress
import com.quizverse.app.data.prepopulate.questions.animalQuestions
import com.quizverse.app.data.prepopulate.questions.animalQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.animalQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.animalQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.animalQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.animalQuestionsEasy6
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMedium6
import com.quizverse.app.data.prepopulate.questions.animalQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.animalQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.animalQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.animalQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.animalQuestionsHard6
import com.quizverse.app.data.prepopulate.questions.animalQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.animalQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.animalQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.animalQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.animalQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.animalQuestionsExpert7
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMaster5
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMaster6
import com.quizverse.app.data.prepopulate.questions.animalQuestionsMaster7
import com.quizverse.app.data.prepopulate.questions.filmQuestions
import com.quizverse.app.data.prepopulate.questions.filmQuestionsEasy
import com.quizverse.app.data.prepopulate.questions.filmQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.filmQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.filmQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.filmQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMedium
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.filmQuestionsHard
import com.quizverse.app.data.prepopulate.questions.filmQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.filmQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.filmQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.filmQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.filmQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.filmQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.filmQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.filmQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.filmQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.filmQuestionsExpert7
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMaster1
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMaster5
import com.quizverse.app.data.prepopulate.questions.filmQuestionsMaster6
import com.quizverse.app.data.prepopulate.questions.foodQuestions
import com.quizverse.app.data.prepopulate.questions.foodQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.geoQuestions
import com.quizverse.app.data.prepopulate.questions.geoQuestionsEasy
import com.quizverse.app.data.prepopulate.questions.geoQuestionsMedium
import com.quizverse.app.data.prepopulate.questions.geoQuestionsHard
import com.quizverse.app.data.prepopulate.questions.geoQuestionsExpert
import com.quizverse.app.data.prepopulate.questions.geoQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.geoQuestionsMaster
import com.quizverse.app.data.prepopulate.questions.geoQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.geoQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.geoQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.geoQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.historyQuestions
import com.quizverse.app.data.prepopulate.questions.historyQuestionsEasy
import com.quizverse.app.data.prepopulate.questions.historyQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.historyQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.historyQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.historyQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMedium
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.historyQuestionsHard
import com.quizverse.app.data.prepopulate.questions.historyQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.historyQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.historyQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.historyQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.historyQuestionsExpert
import com.quizverse.app.data.prepopulate.questions.historyQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.historyQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.historyQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.historyQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.historyQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMaster
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMaster5
import com.quizverse.app.data.prepopulate.questions.historyQuestionsMaster6
import com.quizverse.app.data.prepopulate.questions.literatureQuestions
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsEasy6
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMedium1
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsHard1
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsExpert1
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMaster1
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMaster5
import com.quizverse.app.data.prepopulate.questions.literatureQuestionsMaster6
import com.quizverse.app.data.prepopulate.questions.logicQuestions
import com.quizverse.app.data.prepopulate.questions.mixedQuestions
import com.quizverse.app.data.prepopulate.questions.musicQuestions
import com.quizverse.app.data.prepopulate.questions.musicQuestionsEasy
import com.quizverse.app.data.prepopulate.questions.musicQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.musicQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.musicQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.musicQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMedium
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.musicQuestionsHard
import com.quizverse.app.data.prepopulate.questions.musicQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.musicQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.musicQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.musicQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.musicQuestionsExpert
import com.quizverse.app.data.prepopulate.questions.musicQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.musicQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.musicQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.musicQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.musicQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.musicQuestionsExpert7
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMaster
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMaster5
import com.quizverse.app.data.prepopulate.questions.musicQuestionsMaster6
import com.quizverse.app.data.prepopulate.questions.scienceQuestions
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsEasy
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMedium
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsHard
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsExpert
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMaster
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMaster5
import com.quizverse.app.data.prepopulate.questions.scienceQuestionsMaster6
import com.quizverse.app.data.prepopulate.questions.sportQuestions
import com.quizverse.app.data.prepopulate.questions.sportQuestionsEasy
import com.quizverse.app.data.prepopulate.questions.sportQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.sportQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.sportQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.sportQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.sportQuestionsExpert
import com.quizverse.app.data.prepopulate.questions.sportQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.sportQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.sportQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.sportQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.sportQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.sportQuestionsHard
import com.quizverse.app.data.prepopulate.questions.sportQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.sportQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.sportQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.sportQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.sportQuestionsHard6
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMedium
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMaster
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMaster5
import com.quizverse.app.data.prepopulate.questions.sportQuestionsMaster6
import com.quizverse.app.data.prepopulate.questions.techQuestions
import com.quizverse.app.data.prepopulate.questions.techQuestionsEasy
import com.quizverse.app.data.prepopulate.questions.techQuestionsEasy1
import com.quizverse.app.data.prepopulate.questions.techQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.techQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.techQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.techQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.techQuestionsMedium
import com.quizverse.app.data.prepopulate.questions.techQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.techQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.techQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.techQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.techQuestionsHard
import com.quizverse.app.data.prepopulate.questions.techQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.techQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.techQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.techQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.techQuestionsExpert
import com.quizverse.app.data.prepopulate.questions.techQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.techQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.techQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.techQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.techQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.techQuestionsMaster1
import com.quizverse.app.data.prepopulate.questions.techQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.techQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.techQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.techQuestionsMaster5
import com.quizverse.app.data.prepopulate.questions.techQuestionsMaster6
import com.quizverse.app.data.prepopulate.questions.healthQuestionsEasy1
import com.quizverse.app.data.prepopulate.questions.healthQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.healthQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.healthQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.healthQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.healthQuestionsEasy6
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMedium1
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMedium6
import com.quizverse.app.data.prepopulate.questions.healthQuestionsHard1
import com.quizverse.app.data.prepopulate.questions.healthQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.healthQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.healthQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.healthQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.healthQuestionsHard6
import com.quizverse.app.data.prepopulate.questions.healthQuestionsExpert1
import com.quizverse.app.data.prepopulate.questions.healthQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.healthQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.healthQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.healthQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.healthQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.healthQuestionsExpert7
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMaster1
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMaster5
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMaster6
import com.quizverse.app.data.prepopulate.questions.healthQuestionsMaster7
import com.quizverse.app.data.prepopulate.questions.foodQuestionsEasy1
import com.quizverse.app.data.prepopulate.questions.foodQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.foodQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.foodQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.foodQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMedium6
import com.quizverse.app.data.prepopulate.questions.foodQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.foodQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.foodQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.foodQuestionsHard6
import com.quizverse.app.data.prepopulate.questions.foodQuestionsExpert1
import com.quizverse.app.data.prepopulate.questions.foodQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.foodQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.foodQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.foodQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.foodQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMaster1
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMaster3
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMaster4
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMaster5
import com.quizverse.app.data.prepopulate.questions.foodQuestionsMaster6
import com.quizverse.app.data.prepopulate.questions.logicQuestionsEasy1
import com.quizverse.app.data.prepopulate.questions.logicQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.logicQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.logicQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.logicQuestionsMedium1
import com.quizverse.app.data.prepopulate.questions.logicQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.logicQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.logicQuestionsHard1
import com.quizverse.app.data.prepopulate.questions.logicQuestionsExpert1
import com.quizverse.app.data.prepopulate.questions.logicQuestionsMaster1
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestions
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsEasy6
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsMedium6
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsHard1
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsExpert1
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsMaster1
import com.quizverse.app.data.prepopulate.questions.herthaBscQuestionsMaster2
import com.quizverse.app.data.prepopulate.questions.dortmundQuestions
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsEasy2
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsEasy3
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsEasy4
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsEasy5
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsEasy6
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsMedium1
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsMedium2
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsMedium3
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsMedium4
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsMedium5
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsHard1
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsHard2
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsHard3
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsHard4
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsHard5
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsExpert1
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsExpert2
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsExpert3
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsExpert4
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsExpert5
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsExpert6
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsMaster1
import com.quizverse.app.data.prepopulate.questions.dortmundQuestionsMaster2

/**
 * Seeds the database with initial data (categories, questions, achievements, user progress).
 * Called exactly once from [QuizDatabase.DatabaseCallback.onCreate].
 */
object QuestionSeeder {

    suspend fun seedDatabase(database: QuizDatabase) {
        insertCategories(database)
        insertQuestions(database)
        insertAchievements(database)
        insertInitialProgress(database)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Categories
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun insertCategories(database: QuizDatabase) {
        // Build list of all 12 categories; questionCount reflects seeded data (50 each).
        val categories = listOf(
            Category(id = 1,  name = "Weltgeographie",      iconName = "\uD83C\uDF0D", gradientStartColor = 0xFF4FACFEL, gradientEndColor = 0xFF00F2FEL, questionCount = 1424, isLocked = false),
            Category(id = 2,  name = "Wissenschaft & Natur",iconName = "\uD83D\uDD2C", gradientStartColor = 0xFF6C63FFL, gradientEndColor = 0xFFA855F7L, questionCount = 1500, isLocked = false),
            Category(id = 3,  name = "Geschichte",          iconName = "\uD83D\uDCDC", gradientStartColor = 0xFFF97316L, gradientEndColor = 0xFFFBBF24L, questionCount = 1500, isLocked = false),
            Category(id = 4,  name = "Film & Fernsehen",    iconName = "\uD83C\uDFAC", gradientStartColor = 0xFFEC4899L, gradientEndColor = 0xFFF43F5EL, questionCount = 560, isLocked = false),
            Category(id = 5,  name = "Musik",               iconName = "\uD83C\uDFB5", gradientStartColor = 0xFF8B5CF6L, gradientEndColor = 0xFFC084FCL, questionCount = 350, isLocked = false),
            Category(id = 6,  name = "Sport",               iconName = "\uD83C\uDFC5", gradientStartColor = 0xFF22C55EL, gradientEndColor = 0xFF10B981L, questionCount = 1500, isLocked = false),
            Category(id = 7,  name = "Technologie",         iconName = "\uD83D\uDCBB", gradientStartColor = 0xFF3B82F6L, gradientEndColor = 0xFF6366F1L, questionCount = 1500, isLocked = false),
            Category(id = 8,  name = "Essen & Trinken",     iconName = "\uD83C\uDF73", gradientStartColor = 0xFFF59E0BL, gradientEndColor = 0xFFEF4444L, questionCount = 50, isLocked = false),
            Category(id = 9,  name = "Tierwelt",            iconName = "\uD83D\uDC3E", gradientStartColor = 0xFF14B8A6L, gradientEndColor = 0xFF06B6D4L, questionCount = 1500, isLocked = false),
            Category(id = 10, name = "Sprache & Literatur", iconName = "\uD83D\uDCDA", gradientStartColor = 0xFF8B5CF6L, gradientEndColor = 0xFFEC4899L, questionCount = 50, isLocked = false),
            Category(id = 11, name = "Alle Kategorien",      iconName = "\uD83C\uDF1F", gradientStartColor = 0xFFF43F5EL, gradientEndColor = 0xFFF97316L, questionCount = 50, isLocked = false),
            Category(id = 12, name = "Logik & Denksport",   iconName = "\uD83E\uDDE0", gradientStartColor = 0xFF0EA5E9L, gradientEndColor = 0xFF6366F1L, questionCount = 50, isLocked = false),
            // ── Bundesliga-Vereine (Unterkategorien von Sport) ──────────
            Category(id = 13, name = "Hertha BSC",          iconName = "\u26BD",       gradientStartColor = 0xFF1A5276L, gradientEndColor = 0xFF2E86C1L, questionCount = 50, isLocked = false),
            Category(id = 14, name = "Borussia Dortmund",   iconName = "\u26BD",       gradientStartColor = 0xFFFDD835L, gradientEndColor = 0xFF212121L, questionCount = 50, isLocked = false),
            // ── Neue Kategorien ───────────────────────────────────────
            Category(id = 15, name = "Fußball",              iconName = "\u26BD",       gradientStartColor = 0xFF059669L, gradientEndColor = 0xFF34D399L, questionCount = 0, isLocked = false),
            Category(id = 16, name = "Gesundheit & Medizin", iconName = "\uD83C\uDFE5", gradientStartColor = 0xFFEF4444L, gradientEndColor = 0xFFF97316L, questionCount = 1500, isLocked = false),
            Category(id = 17, name = "Politik & Gesellschaft",iconName = "\uD83C\uDFDB", gradientStartColor = 0xFF6366F1L, gradientEndColor = 0xFF818CF8L, questionCount = 0, isLocked = false),
        )

        // Insert categories using raw SQLite through the support DB to avoid a missing CategoryDao.
        categories.forEach { cat ->
            database.openHelper.writableDatabase.execSQL(
                """INSERT OR REPLACE INTO categories
                   (id, name, iconName, gradientStartColor, gradientEndColor, questionCount, isLocked)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                arrayOf(cat.id, cat.name, cat.iconName, cat.gradientStartColor, cat.gradientEndColor, cat.questionCount, if (cat.isLocked) 1 else 0)
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Questions  (50 per category, 600 total)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun insertQuestions(database: QuizDatabase) {
        val dao = database.questionDao()
        val all = geoQuestions() +
            geoQuestionsEasy() +
            geoQuestionsMedium() +
            geoQuestionsHard() +
            geoQuestionsExpert() +
            geoQuestionsExpert2() +
            geoQuestionsMaster() +
            geoQuestionsMaster2() +
            geoQuestionsMaster3() +
            geoQuestionsMaster4() +
            geoQuestionsMedium2() +
            scienceQuestions() +
            scienceQuestionsEasy() +
            scienceQuestionsEasy2() +
            scienceQuestionsEasy3() +
            scienceQuestionsEasy4() +
            scienceQuestionsEasy5() +
            scienceQuestionsMedium() +
            scienceQuestionsMedium2() +
            scienceQuestionsMedium3() +
            scienceQuestionsMedium4() +
            scienceQuestionsMedium5() +
            scienceQuestionsHard() +
            scienceQuestionsHard2() +
            scienceQuestionsHard3() +
            scienceQuestionsHard4() +
            scienceQuestionsHard5() +
            scienceQuestionsExpert() +
            scienceQuestionsExpert2() +
            scienceQuestionsExpert3() +
            scienceQuestionsExpert4() +
            scienceQuestionsExpert5() +
            scienceQuestionsExpert6() +
            scienceQuestionsMaster() +
            scienceQuestionsMaster2() +
            scienceQuestionsMaster3() +
            scienceQuestionsMaster4() +
            scienceQuestionsMaster5() +
            scienceQuestionsMaster6() +
            historyQuestions() +
            historyQuestionsEasy() +
            historyQuestionsEasy2() +
            historyQuestionsEasy3() +
            historyQuestionsEasy4() +
            historyQuestionsEasy5() +
            historyQuestionsMedium() +
            historyQuestionsMedium2() +
            historyQuestionsMedium3() +
            historyQuestionsMedium4() +
            historyQuestionsMedium5() +
            historyQuestionsHard() +
            historyQuestionsHard2() +
            historyQuestionsHard3() +
            historyQuestionsHard4() +
            historyQuestionsHard5() +
            historyQuestionsExpert() +
            historyQuestionsExpert2() +
            historyQuestionsExpert3() +
            historyQuestionsExpert4() +
            historyQuestionsExpert5() +
            historyQuestionsExpert6() +
            historyQuestionsMaster() +
            historyQuestionsMaster2() +
            historyQuestionsMaster3() +
            historyQuestionsMaster4() +
            historyQuestionsMaster5() +
            historyQuestionsMaster6() +
            filmQuestions() +
            filmQuestionsEasy() +
            filmQuestionsEasy2() +
            filmQuestionsEasy3() +
            filmQuestionsEasy4() +
            filmQuestionsEasy5() +
            filmQuestionsMedium() +
            filmQuestionsMedium2() +
            filmQuestionsMedium3() +
            filmQuestionsMedium4() +
            filmQuestionsMedium5() +
            filmQuestionsHard() +
            filmQuestionsHard2() +
            filmQuestionsHard3() +
            filmQuestionsHard4() +
            filmQuestionsHard5() +
            filmQuestionsExpert2() +
            filmQuestionsExpert3() +
            filmQuestionsExpert4() +
            filmQuestionsExpert5() +
            filmQuestionsExpert6() +
            filmQuestionsExpert7() +
            filmQuestionsMaster1() +
            filmQuestionsMaster2() +
            filmQuestionsMaster3() +
            filmQuestionsMaster4() +
            filmQuestionsMaster5() +
            filmQuestionsMaster6() +
            musicQuestions() +
            musicQuestionsEasy() +
            musicQuestionsEasy2() +
            musicQuestionsEasy3() +
            musicQuestionsEasy4() +
            musicQuestionsEasy5() +
            musicQuestionsMedium() +
            musicQuestionsMedium2() +
            musicQuestionsMedium3() +
            musicQuestionsMedium4() +
            musicQuestionsMedium5() +
            musicQuestionsHard() +
            musicQuestionsHard2() +
            musicQuestionsHard3() +
            musicQuestionsHard4() +
            musicQuestionsHard5() +
            musicQuestionsExpert() +
            musicQuestionsExpert2() +
            musicQuestionsExpert3() +
            musicQuestionsExpert4() +
            musicQuestionsExpert5() +
            musicQuestionsExpert6() +
            musicQuestionsExpert7() +
            musicQuestionsMaster() +
            musicQuestionsMaster2() +
            musicQuestionsMaster3() +
            musicQuestionsMaster4() +
            musicQuestionsMaster5() +
            musicQuestionsMaster6() +
            sportQuestions() +
            sportQuestionsEasy() +
            sportQuestionsEasy2() +
            sportQuestionsEasy3() +
            sportQuestionsEasy4() +
            sportQuestionsEasy5() +
            sportQuestionsExpert() +
            sportQuestionsExpert2() +
            sportQuestionsExpert3() +
            sportQuestionsExpert4() +
            sportQuestionsExpert5() +
            sportQuestionsExpert6() +
            sportQuestionsHard() +
            sportQuestionsHard2() +
            sportQuestionsHard3() +
            sportQuestionsHard4() +
            sportQuestionsHard5() +
            sportQuestionsHard6() +
            sportQuestionsMedium() +
            sportQuestionsMedium2() +
            sportQuestionsMedium3() +
            sportQuestionsMedium4() +
            sportQuestionsMedium5() +
            sportQuestionsMaster() +
            sportQuestionsMaster2() +
            sportQuestionsMaster3() +
            sportQuestionsMaster4() +
            sportQuestionsMaster5() +
            sportQuestionsMaster6() +
            techQuestions() +
            techQuestionsEasy() +
            techQuestionsEasy1() +
            techQuestionsEasy2() +
            techQuestionsEasy3() +
            techQuestionsEasy4() +
            techQuestionsEasy5() +
            techQuestionsMedium() +
            techQuestionsMedium2() +
            techQuestionsMedium3() +
            techQuestionsMedium4() +
            techQuestionsMedium5() +
            techQuestionsHard() +
            techQuestionsHard2() +
            techQuestionsHard3() +
            techQuestionsHard4() +
            techQuestionsHard5() +
            techQuestionsExpert() +
            techQuestionsExpert2() +
            techQuestionsExpert3() +
            techQuestionsExpert4() +
            techQuestionsExpert5() +
            techQuestionsExpert6() +
            techQuestionsMaster1() +
            techQuestionsMaster2() +
            techQuestionsMaster3() +
            techQuestionsMaster4() +
            techQuestionsMaster5() +
            techQuestionsMaster6() +
            foodQuestions() +
            foodQuestionsEasy1() +
            foodQuestionsEasy2() +
            foodQuestionsEasy3() +
            foodQuestionsEasy4() +
            foodQuestionsMedium2() +
            foodQuestionsHard2() +
            foodQuestionsEasy5() +
            foodQuestionsMedium3() +
            foodQuestionsMedium4() +
            foodQuestionsMedium5() +
            foodQuestionsMedium6() +
            foodQuestionsHard3() +
            foodQuestionsHard4() +
            foodQuestionsHard5() +
            foodQuestionsHard6() +
            foodQuestionsExpert1() +
            foodQuestionsExpert2() +
            foodQuestionsExpert3() +
            foodQuestionsExpert4() +
            foodQuestionsExpert5() +
            foodQuestionsExpert6() +
            foodQuestionsMaster1() +
            foodQuestionsMaster2() +
            foodQuestionsMaster3() +
            foodQuestionsMaster4() +
            foodQuestionsMaster5() +
            foodQuestionsMaster6() +
            animalQuestions() +
            animalQuestionsEasy2() +
            animalQuestionsEasy3() +
            animalQuestionsEasy4() +
            animalQuestionsEasy5() +
            animalQuestionsEasy6() +
            animalQuestionsMedium2() +
            animalQuestionsMedium3() +
            animalQuestionsMedium4() +
            animalQuestionsMedium5() +
            animalQuestionsMedium6() +
            animalQuestionsHard2() +
            animalQuestionsHard3() +
            animalQuestionsHard4() +
            animalQuestionsHard5() +
            animalQuestionsHard6() +
            animalQuestionsExpert2() +
            animalQuestionsExpert3() +
            animalQuestionsExpert4() +
            animalQuestionsExpert5() +
            animalQuestionsExpert6() +
            animalQuestionsExpert7() +
            animalQuestionsMaster2() +
            animalQuestionsMaster3() +
            animalQuestionsMaster4() +
            animalQuestionsMaster5() +
            animalQuestionsMaster6() +
            animalQuestionsMaster7() +
            literatureQuestions() +
            literatureQuestionsEasy2() +
            literatureQuestionsEasy3() +
            literatureQuestionsEasy4() +
            literatureQuestionsEasy5() +
            literatureQuestionsEasy6() +
            literatureQuestionsMedium1() +
            literatureQuestionsMedium2() +
            literatureQuestionsMedium3() +
            literatureQuestionsMedium4() +
            literatureQuestionsMedium5() +
            literatureQuestionsHard1() +
            literatureQuestionsHard2() +
            literatureQuestionsHard3() +
            literatureQuestionsHard4() +
            literatureQuestionsHard5() +
            literatureQuestionsExpert1() +
            literatureQuestionsExpert2() +
            literatureQuestionsExpert3() +
            literatureQuestionsExpert4() +
            literatureQuestionsExpert5() +
            literatureQuestionsExpert6() +
            literatureQuestionsMaster1() +
            literatureQuestionsMaster2() +
            literatureQuestionsMaster3() +
            literatureQuestionsMaster4() +
            literatureQuestionsMaster5() +
            literatureQuestionsMaster6() +
            mixedQuestions() +
            logicQuestions() +
            logicQuestionsEasy1() +
            logicQuestionsEasy2() +
            logicQuestionsEasy3() +
            logicQuestionsEasy4() +
            logicQuestionsMedium1() +
            logicQuestionsMedium2() +
            logicQuestionsMedium3() +
            logicQuestionsHard1() +
            logicQuestionsExpert1() +
            logicQuestionsMaster1() +
            herthaBscQuestions() +
            herthaBscQuestionsEasy2() +
            herthaBscQuestionsEasy3() +
            herthaBscQuestionsEasy4() +
            herthaBscQuestionsEasy5() +
            herthaBscQuestionsEasy6() +
            herthaBscQuestionsMedium2() +
            herthaBscQuestionsMedium3() +
            herthaBscQuestionsMedium4() +
            herthaBscQuestionsMedium5() +
            herthaBscQuestionsMedium6() +
            herthaBscQuestionsHard1() +
            herthaBscQuestionsHard2() +
            herthaBscQuestionsHard3() +
            herthaBscQuestionsHard4() +
            herthaBscQuestionsHard5() +
            herthaBscQuestionsExpert1() +
            herthaBscQuestionsExpert2() +
            herthaBscQuestionsExpert3() +
            herthaBscQuestionsExpert4() +
            herthaBscQuestionsExpert5() +
            herthaBscQuestionsExpert6() +
            herthaBscQuestionsMaster1() +
            herthaBscQuestionsMaster2() +
            dortmundQuestions() +
            dortmundQuestionsEasy2() +
            dortmundQuestionsEasy3() +
            dortmundQuestionsEasy4() +
            dortmundQuestionsEasy5() +
            dortmundQuestionsEasy6() +
            dortmundQuestionsMedium1() +
            dortmundQuestionsMedium2() +
            dortmundQuestionsMedium3() +
            dortmundQuestionsMedium4() +
            dortmundQuestionsMedium5() +
            dortmundQuestionsHard1() +
            dortmundQuestionsHard2() +
            dortmundQuestionsHard3() +
            dortmundQuestionsHard4() +
            dortmundQuestionsHard5() +
            dortmundQuestionsExpert1() +
            dortmundQuestionsExpert2() +
            dortmundQuestionsExpert3() +
            dortmundQuestionsExpert4() +
            dortmundQuestionsExpert5() +
            dortmundQuestionsExpert6() +
            dortmundQuestionsMaster1() +
            dortmundQuestionsMaster2() +
            // ── Health & Medicine (categoryId = 16) ──────────────────────
            healthQuestionsEasy1() +
            healthQuestionsEasy2() +
            healthQuestionsEasy3() +
            healthQuestionsEasy4() +
            healthQuestionsEasy5() +
            healthQuestionsEasy6() +
            healthQuestionsMedium1() +
            healthQuestionsMedium2() +
            healthQuestionsMedium3() +
            healthQuestionsMedium4() +
            healthQuestionsMedium5() +
            healthQuestionsMedium6() +
            healthQuestionsHard1() +
            healthQuestionsHard2() +
            healthQuestionsHard3() +
            healthQuestionsHard4() +
            healthQuestionsHard5() +
            healthQuestionsHard6() +
            healthQuestionsExpert1() +
            healthQuestionsExpert2() +
            healthQuestionsExpert3() +
            healthQuestionsExpert4() +
            healthQuestionsExpert5() +
            healthQuestionsExpert6() +
            healthQuestionsMaster1() +
            healthQuestionsMaster2() +
            healthQuestionsMaster3() +
            healthQuestionsMaster4() +
            healthQuestionsMaster5() +
            healthQuestionsMaster6() +
            healthQuestionsExpert7() +
            healthQuestionsMaster7()
        dao.insertAll(all)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Achievements (40+ unique milestones across multiple categories)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun insertAchievements(database: QuizDatabase) {
        val dao = database.achievementDao()
        val achievements = listOf(
            // ── Erste Schritte ─────────────────────────────────────────────
            Achievement(id = "first_quiz",       nameKey = "Erster Start",          descriptionKey = "Schließe dein erstes Quiz ab",                  iconName = "\uD83C\uDFAF", requiredValue = 1,   tier = 1),
            Achievement(id = "first_correct",    nameKey = "Erste Antwort",         descriptionKey = "Beantworte deine erste Frage richtig",           iconName = "\u2705",       requiredValue = 1,   tier = 1),
            Achievement(id = "first_perfect",    nameKey = "Fehlerlos",             descriptionKey = "Beende ein Quiz ohne Fehler",                    iconName = "\u2B50",       requiredValue = 1,   tier = 2),
            Achievement(id = "quiz_5",           nameKey = "Aufgewärmt",            descriptionKey = "Schließe 5 Quizzes ab",                          iconName = "\uD83D\uDD25", requiredValue = 5,   tier = 1),
            Achievement(id = "quiz_25",          nameKey = "Quiz-Enthusiast",       descriptionKey = "Schließe 25 Quizzes ab",                         iconName = "\uD83D\uDCCB", requiredValue = 25,  tier = 2),
            Achievement(id = "quiz_100",         nameKey = "Quiz-Profi",            descriptionKey = "Schließe 100 Quizzes ab",                        iconName = "\uD83C\uDFC6", requiredValue = 100, tier = 3),
            Achievement(id = "quiz_500",         nameKey = "Quiz-Legende",          descriptionKey = "Schließe 500 Quizzes ab",                        iconName = "\uD83D\uDC51", requiredValue = 500, tier = 3),

            // ── Streak-Achievements ────────────────────────────────────────
            Achievement(id = "streak_3",         nameKey = "3er-Strähne",           descriptionKey = "Erreiche einen Streak von 3",                    iconName = "\uD83D\uDD25", requiredValue = 3,   tier = 1),
            Achievement(id = "streak_7",         nameKey = "Eine Woche Feuer",      descriptionKey = "Erreiche einen Streak von 7",                    iconName = "\uD83D\uDD25", requiredValue = 7,   tier = 1),
            Achievement(id = "streak_10",        nameKey = "Flamme",                descriptionKey = "Erreiche einen Streak von 10",                   iconName = "\uD83D\uDD25", requiredValue = 10,  tier = 2),
            Achievement(id = "streak_25",        nameKey = "Inferno",               descriptionKey = "Erreiche einen Streak von 25",                   iconName = "\uD83C\uDF0B", requiredValue = 25,  tier = 2),
            Achievement(id = "streak_50",        nameKey = "Feuersturm",            descriptionKey = "Erreiche einen Streak von 50",                   iconName = "\uD83C\uDF2A", requiredValue = 50,  tier = 3),
            Achievement(id = "streak_100",       nameKey = "Ewige Flamme",          descriptionKey = "Erreiche einen Streak von 100",                  iconName = "\u2600",       requiredValue = 100, tier = 3),

            // ── Geschwindigkeit ────────────────────────────────────────────
            Achievement(id = "speed_demon",      nameKey = "Blitzschnell",          descriptionKey = "Beantworte 5 Fragen in unter 3 Sekunden",        iconName = "\u26A1",       requiredValue = 5,   tier = 2),
            Achievement(id = "speed_master",     nameKey = "Gedanken-Blitz",        descriptionKey = "Beantworte 20 Fragen in unter 3 Sekunden",       iconName = "\uD83C\uDF29", requiredValue = 20,  tier = 3),
            Achievement(id = "quick_10",         nameKey = "Schnelldenker",         descriptionKey = "Beende ein 10-Fragen-Quiz in unter 60 Sekunden", iconName = "\u23F1",       requiredValue = 1,   tier = 2),

            // ── Richtige Antworten ─────────────────────────────────────────
            Achievement(id = "correct_10",       nameKey = "Wissenshungrig",        descriptionKey = "Beantworte 10 Fragen richtig",                   iconName = "\uD83D\uDCDA", requiredValue = 10,  tier = 1),
            Achievement(id = "correct_50",       nameKey = "Wissbegierig",          descriptionKey = "Beantworte 50 Fragen richtig",                   iconName = "\uD83D\uDCD6", requiredValue = 50,  tier = 1),
            Achievement(id = "correct_100",      nameKey = "Experte",               descriptionKey = "Beantworte 100 Fragen richtig",                  iconName = "\uD83C\uDF93", requiredValue = 100, tier = 2),
            Achievement(id = "correct_500",      nameKey = "Meister",               descriptionKey = "Beantworte 500 Fragen richtig",                  iconName = "\uD83E\uDDD9", requiredValue = 500, tier = 2),
            Achievement(id = "marathon_1000",    nameKey = "Marathon",              descriptionKey = "Beantworte 1000 Fragen gesamt",                  iconName = "\uD83C\uDFC3", requiredValue = 1000, tier = 3),
            Achievement(id = "marathon_100",     nameKey = "Ausdauerläufer",        descriptionKey = "Beantworte 100 Fragen in einer Session",         iconName = "\uD83D\uDCAA", requiredValue = 100, tier = 2),

            // ── XP & Level ────────────────────────────────────────────────
            Achievement(id = "level_5",          nameKey = "Aufsteiger",            descriptionKey = "Erreiche Level 5",                               iconName = "\uD83D\uDCC8", requiredValue = 5,   tier = 1),
            Achievement(id = "level_10",         nameKey = "Fortgeschrittener",     descriptionKey = "Erreiche Level 10",                              iconName = "\uD83D\uDCCA", requiredValue = 10,  tier = 2),
            Achievement(id = "level_25",         nameKey = "Veteran",               descriptionKey = "Erreiche Level 25",                              iconName = "\uD83E\uDD48", requiredValue = 25,  tier = 2),
            Achievement(id = "level_50",         nameKey = "Elite",                 descriptionKey = "Erreiche Level 50",                              iconName = "\uD83E\uDD47", requiredValue = 50,  tier = 3),
            Achievement(id = "xp_1000",          nameKey = "XP-Sammler",            descriptionKey = "Sammle 1.000 XP",                                iconName = "\uD83D\uDC8E", requiredValue = 1000, tier = 1),
            Achievement(id = "xp_10000",         nameKey = "XP-Millionär (Kilo)",   descriptionKey = "Sammle 10.000 XP",                               iconName = "\uD83D\uDCB0", requiredValue = 10000, tier = 2),
            Achievement(id = "xp_100000",        nameKey = "XP-Legende",            descriptionKey = "Sammle 100.000 XP",                              iconName = "\uD83C\uDF1F", requiredValue = 100000, tier = 3),

            // ── Kategorien ────────────────────────────────────────────────
            Achievement(id = "cat_geo",          nameKey = "Geograf",               descriptionKey = "Schließe 10 Geographie-Quizzes ab",              iconName = "\uD83C\uDF0D", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_science",      nameKey = "Wissenschaftler",       descriptionKey = "Schließe 10 Wissenschafts-Quizzes ab",           iconName = "\uD83D\uDD2C", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_history",      nameKey = "Historiker",            descriptionKey = "Schließe 10 Geschichts-Quizzes ab",              iconName = "\uD83D\uDCDC", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_film",         nameKey = "Filmkenner",            descriptionKey = "Schließe 10 Film & TV-Quizzes ab",               iconName = "\uD83C\uDFAC", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_music",        nameKey = "Musikliebhaber",        descriptionKey = "Schließe 10 Musik-Quizzes ab",                   iconName = "\uD83C\uDFB5", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_sport",        nameKey = "Sportfan",              descriptionKey = "Schließe 10 Sport-Quizzes ab",                   iconName = "\u26BD",       requiredValue = 10,  tier = 1),
            Achievement(id = "cat_tech",         nameKey = "Technikfreak",          descriptionKey = "Schließe 10 Technologie-Quizzes ab",             iconName = "\uD83D\uDCBB", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_food",         nameKey = "Feinschmecker",         descriptionKey = "Schließe 10 Essen & Trinken-Quizzes ab",         iconName = "\uD83C\uDF73", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_animals",      nameKey = "Tierfreund",            descriptionKey = "Schließe 10 Tierwelt-Quizzes ab",                iconName = "\uD83D\uDC3E", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_literature",   nameKey = "Bücherwurm",            descriptionKey = "Schließe 10 Literatur-Quizzes ab",               iconName = "\uD83D\uDCDA", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_mixed",        nameKey = "Allrounder",            descriptionKey = "Schließe 10 Alle-Kategorien-Quizzes ab",         iconName = "\uD83C\uDF1F", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_logic",        nameKey = "Logiker",               descriptionKey = "Schließe 10 Logik-Quizzes ab",                   iconName = "\uD83E\uDDE0", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_football",    nameKey = "Fußball-Experte",       descriptionKey = "Schließe 10 Fußball-Quizzes ab",                 iconName = "\u26BD",       requiredValue = 10,  tier = 1),
            Achievement(id = "cat_health",      nameKey = "Gesundheitsguru",       descriptionKey = "Schließe 10 Gesundheits-Quizzes ab",              iconName = "\uD83C\uDFE5", requiredValue = 10,  tier = 1),
            Achievement(id = "cat_politics",    nameKey = "Politikkenner",         descriptionKey = "Schließe 10 Politik-Quizzes ab",                  iconName = "\uD83C\uDFDB", requiredValue = 10,  tier = 1),
            Achievement(id = "all_cats",         nameKey = "Universalgelehrter",    descriptionKey = "Schließe jede Kategorie mindestens einmal ab",   iconName = "\uD83C\uDF10", requiredValue = 15,  tier = 3),

            // ── Tages-Challenge ───────────────────────────────────────────
            Achievement(id = "daily_3",          nameKey = "Dreifach-Tagesquiz",    descriptionKey = "Löse 3 Tages-Challenges",                        iconName = "\uD83D\uDCC5", requiredValue = 3,   tier = 1),
            Achievement(id = "daily_7",          nameKey = "Wöchentliches Ritual",  descriptionKey = "Löse 7 Tages-Challenges in Folge",               iconName = "\uD83D\uDCC6", requiredValue = 7,   tier = 2),
            Achievement(id = "daily_30",         nameKey = "Monatlicher Meister",   descriptionKey = "Löse 30 Tages-Challenges in Folge",              iconName = "\uD83D\uDDD3", requiredValue = 30,  tier = 3),

            // ── Schwierigkeitslevel ────────────────────────────────────────
            Achievement(id = "hard_10",          nameKey = "Herausforderungsannahme",descriptionKey = "Beantworte 10 schwere Fragen richtig",          iconName = "\uD83D\uDCAA", requiredValue = 10,  tier = 2),
            Achievement(id = "master_5",         nameKey = "Meisterfragen",         descriptionKey = "Beantworte 5 Master-Fragen richtig",             iconName = "\uD83E\uDDE0", requiredValue = 5,   tier = 3),
            Achievement(id = "perfect_hard",     nameKey = "Perfektionist",         descriptionKey = "Beende ein Schwer-Quiz mit 100%",                iconName = "\uD83D\uDCAF", requiredValue = 1,   tier = 3),

            // ── Spielzeit ─────────────────────────────────────────────────
            Achievement(id = "playtime_1h",      nameKey = "Eine Stunde",           descriptionKey = "Spiele insgesamt 1 Stunde",                      iconName = "\u231B",       requiredValue = 60,  tier = 1),
            Achievement(id = "playtime_10h",     nameKey = "Zehn Stunden",          descriptionKey = "Spiele insgesamt 10 Stunden",                    iconName = "\uD83D\uDD50", requiredValue = 600, tier = 2),
            Achievement(id = "playtime_100h",    nameKey = "Hundert Stunden",       descriptionKey = "Spiele insgesamt 100 Stunden",                   iconName = "\uD83C\uDFC5", requiredValue = 6000, tier = 3),

            // ── Sonderpunkte ──────────────────────────────────────────────
            Achievement(id = "night_owl",        nameKey = "Nachteule",             descriptionKey = "Spiele nach Mitternacht",                        iconName = "\uD83E\uDD89", requiredValue = 1,   tier = 1),
            Achievement(id = "early_bird",       nameKey = "Frühaufsteher",         descriptionKey = "Spiele vor 6 Uhr morgens",                       iconName = "\uD83C\uDF05", requiredValue = 1,   tier = 1),
            Achievement(id = "no_hint",          nameKey = "Eigene Kraft",          descriptionKey = "Beende 5 Quizzes ohne Joker",                    iconName = "\uD83E\uDDE9", requiredValue = 5,   tier = 2),
            Achievement(id = "comeback",         nameKey = "Comeback",              descriptionKey = "Gewinne nach 3 falschen Antworten in Folge",     iconName = "\uD83D\uDD04", requiredValue = 1,   tier = 2),
        )
        dao.insertAll(achievements)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Initial UserProgress (empty row for first launch)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun insertInitialProgress(database: QuizDatabase) {
        val dao = database.progressDao()
        dao.insertProgress(UserProgress())
    }
}
