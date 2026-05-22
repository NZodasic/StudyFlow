package com.studyflow.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repositories are concrete classes annotated with @Inject constructor and @Singleton.
    // Hilt handles their instantiation automatically.
}
