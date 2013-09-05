PROJECT = tripmaster
PKG_FILES = www

VERSION := 1.0.0

all: wgtPkg

wgtPkg:
	zip -r $(PROJECT).zip $(PKG_FILES)

